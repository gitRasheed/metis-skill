#ifndef ADC_H
#define ADC_H
#include <stdint.h>
uint16_t adc_pack_mv(void);
int16_t adc_current_ma(void);
int16_t adc_temp_decic(void);
#endif
