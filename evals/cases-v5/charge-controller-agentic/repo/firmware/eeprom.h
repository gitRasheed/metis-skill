#ifndef EEPROM_H
#define EEPROM_H
#include <stdint.h>
uint8_t eeprom_read_phase(void);
void eeprom_write_phase(uint8_t phase);
#endif
