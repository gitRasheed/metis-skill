#ifndef DERATE_H
#define DERATE_H
#include <stdint.h>
int16_t derate_ntc_to_decic(uint16_t raw);
int16_t derate_current_limit(int16_t base_ma, int16_t temp);
#endif
