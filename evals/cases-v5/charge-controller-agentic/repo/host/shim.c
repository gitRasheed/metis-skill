/* Host test shim: the harness sets sensor values and observes outputs.
   This file is fixed infrastructure — do not modify. The firmware links
   against it instead of the MCU ADC/PWM/EEPROM drivers. */
#include <stdint.h>

static uint16_t shim_pack_mv;
static int16_t shim_curr_ma;
static int16_t shim_temp_dc;
static int16_t shim_pwm;
static uint8_t shim_eeprom_phase;
static uint32_t shim_eeprom_writes;

void shim_set_pack_mv(uint16_t v) { shim_pack_mv = v; }
void shim_set_curr_ma(int16_t v)  { shim_curr_ma = v; }
void shim_set_temp_dc(int16_t v)  { shim_temp_dc = v; }
int16_t shim_get_pwm(void)        { return shim_pwm; }
uint32_t shim_get_eeprom_writes(void) { return shim_eeprom_writes; }
uint8_t shim_get_eeprom_phase(void)   { return shim_eeprom_phase; }
void shim_set_eeprom_phase(uint8_t p) { shim_eeprom_phase = p; }

uint16_t adc_pack_mv(void)   { return shim_pack_mv; }
int16_t adc_current_ma(void) { return shim_curr_ma; }
int16_t adc_temp_decic(void) { return shim_temp_dc; }
void pwm_set(int16_t duty)   { shim_pwm = duty; }
uint8_t eeprom_read_phase(void) { return shim_eeprom_phase; }
void eeprom_write_phase(uint8_t phase)
{
    shim_eeprom_phase = phase;
    shim_eeprom_writes++;
}
