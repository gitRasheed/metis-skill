#ifndef PID_H
#define PID_H
#include <stdint.h>
int16_t pid_step(int16_t setpoint, int16_t measured);
void pid_reset(void);
void pid_set_bias(int16_t bias);
void pid_set_output_limits(int16_t minimum, int16_t maximum);
#endif
