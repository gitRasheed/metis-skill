#include <stdint.h>
#include "config.h"

static int16_t integ;
static int16_t prev_err;

int16_t pid_step(int16_t setpoint, int16_t measured)
{
    int16_t err = setpoint - measured;

    integ += (int16_t)(((int32_t)KI_Q15 * err) >> 15);

    int16_t deriv = err - prev_err;
    prev_err = err;

    int32_t out = ((int32_t)KP_Q15 * err) >> 15;
    out += integ;
    out += ((int32_t)KD_Q15 * deriv) >> 15;

    if (out > PWM_MAX) out = PWM_MAX;
    if (out < 0) out = 0;
    return (int16_t)out;
}

void pid_reset(void)
{
    integ = 0;
    prev_err = 0;
}
