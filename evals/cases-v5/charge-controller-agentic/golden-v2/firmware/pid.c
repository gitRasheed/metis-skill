#include <stdint.h>
#include "config.h"

/* Integral is retained in Q15 so sub-count corrections are not discarded. */
static int32_t integral_q15;
static int16_t output_bias;
static int16_t output_min;
static int16_t output_max;

#define INTEGRAL_LIMIT_Q15 ((int32_t)PWM_MAX << 15)

static int32_t clamp_integral(int32_t value)
{
    if (value > INTEGRAL_LIMIT_Q15) {
        return INTEGRAL_LIMIT_Q15;
    }
    if (value < -INTEGRAL_LIMIT_Q15) {
        return -INTEGRAL_LIMIT_Q15;
    }
    return value;
}

static int32_t q15_to_counts(int32_t value)
{
    return value / 32768;
}

int16_t pid_step(int16_t setpoint, int16_t measured)
{
    int32_t error = (int32_t)setpoint - measured;
    int32_t proposed_integral = clamp_integral(
        integral_q15 + (int32_t)KI_Q15 * error
    );
    int32_t correction_q15 = (int32_t)KP_Q15 * error + proposed_integral;
    int32_t output = (int32_t)output_bias + q15_to_counts(correction_q15);

    /* Conditional integration prevents windup at either active limit. */
    if (!((output > output_max && error > 0) ||
          (output < output_min && error < 0))) {
        integral_q15 = proposed_integral;
    } else {
        correction_q15 = (int32_t)KP_Q15 * error + integral_q15;
        output = (int32_t)output_bias + q15_to_counts(correction_q15);
    }

    if (output > output_max) {
        output = output_max;
    }
    if (output < output_min) {
        output = output_min;
    }
    return (int16_t)output;
}

void pid_reset(void)
{
    integral_q15 = 0;
    output_bias = 0;
    output_min = 0;
    output_max = PWM_MAX;
}

void pid_set_bias(int16_t bias)
{
    output_bias = bias;
}

void pid_set_output_limits(int16_t minimum, int16_t maximum)
{
    if (minimum < 0) {
        minimum = 0;
    }
    if (maximum > PWM_MAX) {
        maximum = PWM_MAX;
    }
    if (maximum < minimum) {
        maximum = minimum;
    }
    output_min = minimum;
    output_max = maximum;
}
