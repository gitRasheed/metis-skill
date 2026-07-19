#include <stdint.h>

/* NTC lookup: raw ADC (12-bit) -> tenths of a degree C.
   16 entries spanning the full ADC range; linear interpolation. */
static const int16_t ntc_decic[16] = {
    1250, 1030, 860, 720, 610, 510, 420, 340,
     270,  200, 140,  80,  20, -40, -110, -200
};

int16_t derate_ntc_to_decic(uint16_t raw)
{
    uint8_t idx = (uint8_t)(raw >> 8);
    int16_t a = ntc_decic[idx];
    if (idx == 15) {
        return a;
    }
    int16_t b = ntc_decic[idx + 1];
    uint8_t frac = (uint8_t)(raw & 0xFF);
    return (int16_t)(a + ((b - a) * frac) / 256);
}

/* Current limit vs temperature:
   full current 10..45, half current 0..10 and 45..50, zero outside. */
int16_t derate_current_limit(int16_t base_ma, int16_t temp)
{
    if (temp < 0 || temp > 50) {
        return 0;
    }
    if (temp < 10 || temp > 45) {
        return (int16_t)(base_ma / 2);
    }
    return base_ma;
}
