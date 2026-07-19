#include <stdint.h>
#include "config.h"
#include "adc.h"
#include "derate.h"
#include "eeprom.h"
#include "pid.h"
#include "pwm.h"

enum phase {
    PHASE_IDLE, PHASE_PRECHARGE, PHASE_CC, PHASE_CV,
    PHASE_DONE, PHASE_FAULT
};

static uint8_t phase;
static uint8_t fault_code;

void charger_init(void)
{
    uint8_t saved = eeprom_read_phase();
    if (saved == PHASE_CC || saved == PHASE_CV) {
        phase = saved;          /* pick up where we left off */
    } else {
        phase = PHASE_IDLE;
    }
    fault_code = 0;
}

void charger_tick(void)         /* runs in the control ISR */
{
    uint16_t pack_mv = adc_pack_mv();
    int16_t curr_ma = adc_current_ma();
    int16_t temp_dc = adc_temp_decic();

    int16_t limit_ma = derate_current_limit(I_CC_MA, temp_dc);

    switch (phase) {
    case PHASE_IDLE:
        if (pack_mv > 5000 && temp_dc > 0 && temp_dc < 500) {
            phase = PHASE_PRECHARGE;
        }
        break;
    case PHASE_PRECHARGE:
        pwm_set(pid_step(I_PRECHARGE_MA, curr_ma));
        if (pack_mv >= V_PRECHARGE_MV) {
            phase = PHASE_CC;
        }
        break;
    case PHASE_CC:
        pwm_set(pid_step(limit_ma, curr_ma));
        if (pack_mv >= V_MAX_MV) {
            phase = PHASE_CV;
        }
        break;
    case PHASE_CV:
        pwm_set(pid_step((int16_t)V_MAX_MV, (int16_t)pack_mv));
        if (pack_mv < V_MAX_MV) {
            phase = PHASE_CC;
        }
        if (curr_ma < I_TAPER_MA) {
            phase = PHASE_DONE;
        }
        break;
    case PHASE_DONE:
        pwm_set(0);
        break;
    case PHASE_FAULT:
        pwm_set(0);
        break;
    }

    if (pack_mv > V_MAX_MV + 200 || curr_ma > limit_ma + 500) {
        fault_code = 1;
        phase = PHASE_FAULT;
    }

    eeprom_write_phase(phase);
}
