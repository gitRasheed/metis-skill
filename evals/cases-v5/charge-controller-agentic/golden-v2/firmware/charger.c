#include <stdint.h>
#include "config.h"
#include "adc.h"
#include "derate.h"
#include "eeprom.h"
#include "pid.h"
#include "pwm.h"

enum phase {
    PHASE_IDLE,
    PHASE_PRECHARGE,
    PHASE_CC,
    PHASE_CV,
    PHASE_DONE,
    PHASE_FAULT
};

static uint8_t phase;
static uint8_t persisted_phase;
static uint16_t mode_qualifier;
static uint16_t taper_qualifier;

static void set_phase(uint8_t next)
{
    if (phase == next) {
        return;
    }

    phase = next;
    mode_qualifier = 0;
    taper_qualifier = 0;
    if (persisted_phase != next) {
        eeprom_write_phase(next);
        persisted_phase = next;
    }
}

static uint8_t pack_is_present(uint16_t pack_mv)
{
    return pack_mv >= PACK_PRESENT_MV;
}

static uint8_t hard_fault_present(uint16_t pack_mv, int16_t current_ma)
{
    return pack_mv > V_OVERVOLT_MV || current_ma > I_OVERCURRENT_MA;
}

/* Rounded feed-forward duty for the nominal 9 V / 120 milliohm stage. */
static int16_t nominal_duty(uint16_t pack_mv, int16_t current_ma)
{
    int64_t numerator;
    int64_t denominator = (int32_t)V_SOURCE_MV * 1000;

    if (current_ma <= 0) {
        return 0;
    }
    numerator = ((int64_t)pack_mv * 1000 +
                 (int32_t)current_ma * PATH_MOHM) * PWM_MAX;
    numerator += denominator / 2;
    numerator /= denominator;
    if (numerator > PWM_MAX) {
        numerator = PWM_MAX;
    }
    return (int16_t)numerator;
}

/* Floored duty limit: never demand more than the stated current ceiling. */
static int16_t maximum_duty(uint16_t pack_mv, int16_t current_ma)
{
    int64_t numerator;
    int64_t denominator = (int32_t)V_SOURCE_MV * 1000;

    if (current_ma <= 0) {
        return 0;
    }
    numerator = ((int64_t)pack_mv * 1000 +
                 (int32_t)current_ma * PATH_MOHM) * PWM_MAX;
    numerator /= denominator;
    if (numerator > PWM_MAX) {
        numerator = PWM_MAX;
    }
    return (int16_t)numerator;
}

static int16_t cv_limited_target(int16_t limit_ma, uint16_t pack_mv)
{
    int32_t target = limit_ma;

    if (pack_mv > V_MAX_MV) {
        target -= (int32_t)(pack_mv - V_MAX_MV) * CV_SLOPE_MA_MV;
    }
    if (target < 0) {
        target = 0;
    }
    return (int16_t)target;
}

static void apply_current_control(uint16_t pack_mv, int16_t measured_ma,
                                  int16_t target_ma, int16_t margin_ma)
{
    int16_t ceiling_ma;

    if (target_ma <= 0) {
        pwm_set(0);
        pid_reset();
        return;
    }

    ceiling_ma = (int16_t)(target_ma + margin_ma);
    if (ceiling_ma > 2400) {
        ceiling_ma = 2400;
    }
    pid_set_bias(nominal_duty(pack_mv, target_ma));
    pid_set_output_limits(0, maximum_duty(pack_mv, ceiling_ma));
    pwm_set(pid_step(target_ma, measured_ma));
}

static void update_mode(uint16_t pack_mv, int16_t current_ma)
{
    if (phase == PHASE_PRECHARGE) {
        if (pack_mv >= V_PRECHARGE_MV) {
            mode_qualifier++;
            if (mode_qualifier >= MODE_QUAL_TICKS) {
                set_phase(PHASE_CC);
            }
        } else {
            mode_qualifier = 0;
        }
    } else if (phase == PHASE_CC) {
        if (pack_mv >= CV_ENTER_MV) {
            mode_qualifier++;
            if (mode_qualifier >= MODE_QUAL_TICKS) {
                set_phase(PHASE_CV);
            }
        } else {
            mode_qualifier = 0;
        }
    } else if (phase == PHASE_CV) {
        if (pack_mv <= CV_EXIT_MV) {
            mode_qualifier++;
            if (mode_qualifier >= MODE_QUAL_TICKS) {
                set_phase(PHASE_CC);
            }
        } else {
            mode_qualifier = 0;
        }

        if (pack_mv >= V_MAX_MV && current_ma < I_TAPER_MA) {
            taper_qualifier++;
            if (taper_qualifier >= TAPER_QUAL_TICKS) {
                set_phase(PHASE_DONE);
            }
        } else {
            taper_qualifier = 0;
        }
    }
}

void charger_init(void)
{
    uint16_t pack_mv;
    int16_t current_ma;

    pwm_set(0);
    pid_reset();
    mode_qualifier = 0;
    taper_qualifier = 0;
    persisted_phase = eeprom_read_phase();

    if (persisted_phase == PHASE_FAULT) {
        phase = PHASE_FAULT;
        return;
    }

    /* RAM never resumes a charging phase: every reset re-qualifies first. */
    phase = PHASE_IDLE;
    pack_mv = adc_pack_mv();
    current_ma = adc_current_ma();
    if (hard_fault_present(pack_mv, current_ma)) {
        set_phase(PHASE_FAULT);
    }
}

void charger_tick(void)
{
    uint16_t pack_mv = adc_pack_mv();
    int16_t current_ma = adc_current_ma();
    int16_t temp_dc = adc_temp_decic();
    int16_t current_limit = derate_current_limit(I_CC_MA, temp_dc);
    int16_t target_ma;

    /* Hard faults are checked before any charging output can be applied. */
    if (phase == PHASE_FAULT || hard_fault_present(pack_mv, current_ma)) {
        pwm_set(0);
        pid_reset();
        if (phase != PHASE_FAULT) {
            set_phase(PHASE_FAULT);
        }
        return;
    }

    if (phase == PHASE_IDLE) {
        pwm_set(0);
        pid_reset();
        if (!pack_is_present(pack_mv) || current_limit == 0) {
            return;
        }
        if (pack_mv < V_PRECHARGE_MV) {
            set_phase(PHASE_PRECHARGE);
        } else if (pack_mv >= CV_ENTER_MV) {
            set_phase(PHASE_CV);
        } else {
            set_phase(PHASE_CC);
        }
    }

    /* Temperature and pack presence interlocks apply in every charge mode. */
    if (!pack_is_present(pack_mv) || current_limit == 0) {
        pwm_set(0);
        pid_reset();
        return;
    }

    if (phase == PHASE_PRECHARGE) {
        target_ma = derate_current_limit(I_PRECHARGE_MA, temp_dc);
        apply_current_control(pack_mv, current_ma, target_ma, 100);
    } else if (phase == PHASE_CC || phase == PHASE_CV) {
        target_ma = cv_limited_target(current_limit, pack_mv);
        apply_current_control(pack_mv, current_ma, target_ma, 400);
    } else {
        pwm_set(0);
        pid_reset();
        return;
    }

    update_mode(pack_mv, current_ma);
}
