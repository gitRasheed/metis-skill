#ifndef CONFIG_H
#define CONFIG_H

#define F_CPU_HZ        16000000UL

/* Timer1 CTC tick: 16 MHz / (64 * (312 + 1)) = 798.72 Hz. */
#define TIMER1_PRESCALE 64
#define TIMER1_TOP      312

/*
 * Current-loop correction gains at 798.72 Hz.  Most of the demanded duty is
 * supplied by a power-stage feed-forward term; the PI controller removes
 * modelling and quantisation error.  Keeping the feedback correction gentle
 * is important because one PWM count is about 73 mA in the target plant.
 */
#define KP_Q15          262     /* 0.0080 duty-count / mA */
#define KI_Q15          3       /* 0.0000916 per tick     */

#define I_CC_MA         2000    /* constant-current target        */
#define I_PRECHARGE_MA  200     /* gentle current for a low pack  */
#define I_TAPER_MA      100     /* CV taper -> DONE threshold     */
#define V_MAX_MV        8400    /* 2S li-ion pack ceiling         */
#define V_PRECHARGE_MV  5000    /* precharge -> CC threshold      */

#define V_SOURCE_MV     9000    /* nominal charger rail           */
#define PATH_MOHM       120     /* charger/pack path resistance   */

#define PACK_PRESENT_MV 1000
#define V_OVERVOLT_MV   8600
#define I_OVERCURRENT_MA 2500

#define CV_ENTER_MV     8420
#define CV_EXIT_MV      8380
#define CV_SLOPE_MA_MV  40
#define MODE_QUAL_TICKS 64
#define TAPER_QUAL_TICKS 256

#define PWM_MAX         1023

#endif
