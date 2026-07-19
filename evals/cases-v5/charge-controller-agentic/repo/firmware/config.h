#ifndef CONFIG_H
#define CONFIG_H

#define F_CPU_HZ        16000000UL

/* control tick: TIMER1, 1 kHz -- gains below are tuned per-tick at 1 kHz */
#define TIMER1_PRESCALE 64
#define TIMER1_TOP      312

#define KP_Q15          9830    /* 0.30  */
#define KI_Q15          164     /* 0.005 per tick */
#define KD_Q15          3277    /* 0.10  */

#define I_CC_MA         2000    /* constant-current target        */
#define I_PRECHARGE_MA  200     /* gentle current for a low pack  */
#define I_TAPER_MA      100     /* CV taper -> DONE threshold     */
#define V_MAX_MV        8400    /* 2S li-ion pack ceiling         */
#define V_PRECHARGE_MV  6000    /* precharge -> CC threshold      */

#define PWM_MAX         1023

#endif
