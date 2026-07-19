#ifndef PID_H
#define PID_H
#include <stdint.h>
int16_t pid_step(int16_t setpoint, int16_t measured);
void pid_reset(void);
#endif
