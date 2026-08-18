package com.pratikmardi.equalizerpro

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class MainActivity : Activity() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var visualizer: Visualizer? = null

    private lateinit var spectrum: SpectrumView
    private lateinit var powerButton: Button

    private val sliders = ArrayList<SeekBar>()

    private var enabled = true

    private val frequencies = arrayOf(
        "60", "120", "230", "460", "910",
        "1.8k", "3.6k", "7k", "14k", "16k"
    )

    private val presets = mapOf(
        "Normal" to intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),

        "Rock" to intArrayOf(
            5, 4, 2, -1, -2,
            2, 4, 5, 6, 5
        ),

        "Pop" to intArrayOf(
            -1, 2, 4, 5, 3,
            -1, -2, -1, 3, 4
        ),

        "Classical" to intArrayOf(
            0, 0, -1, -2, 0,
            3, 4, 4, 0, -1
        ),

        "Jazz" to intArrayOf(
            3, 2, -1, -2, -2,
            0, 2, 3, 4, 4
        ),

        "Bass Boost" to intArrayOf(
            8, 7, 6, 4, 2,
            1, 1, 2, 3, 4
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(9, 10, 15)
        window.navigationBarColor = Color.rgb(9, 10, 15)

        buildInterface()
        createAudioEffects()
    }

    private fun buildInterface() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 16, 20, 12)
        root.setBackgroundColor(Color.rgb(9, 10, 15))

        // Header

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val titleBox = LinearLayout(this)

        titleBox.orientation = LinearLayout.VERTICAL

        titleBox.layoutParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        val title = TextView(this)

        title.text = "EQUALIZER PRO"
        title.textSize = 25f
        title.setTextColor(Color.WHITE)

        titleBox.addView(title)

        val subtitle = TextView(this)

        subtitle.text = "10-BAND AUDIO ENHANCER"
        subtitle.textSize = 10f
        subtitle.setTextColor(Color.rgb(0, 229, 255))

        titleBox.addView(subtitle)

        powerButton = Button(this)

        powerButton.text = "ON"
        powerButton.textSize = 12f

        powerButton.setOnClickListener {

            enabled = !enabled

            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled

            powerButton.text =
                if (enabled) "ON" else "OFF"

            powerButton.alpha =
                if (enabled) 1f else 0.5f
        }

        header.addView(titleBox)
        header.addView(powerButton)

        root.addView(header)

        // Spectrum

        spectrum = SpectrumView(this)

        root.addView(
            spectrum,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                210
            ).apply {
                topMargin = 12
                bottomMargin = 10
            }
        )

        // Presets

        val presetScroll = HorizontalScrollView(this)

        val presetRow = LinearLayout(this)

        presetRow.orientation = LinearLayout.HORIZONTAL

        presets.keys.forEach { name ->

            val button = Button(this)

            button.text = name
            button.textSize = 10f

            button.setOnClickListener {
                applyPreset(name)
            }

            presetRow.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    52
                )
            )
        }

        presetScroll.addView(presetRow)

        root.addView(presetScroll)

        // Label

        val bandTitle = TextView(this)

        bandTitle.text = "FREQUENCY BANDS"
        bandTitle.textSize = 11f
        bandTitle.setTextColor(Color.GRAY)

        bandTitle.setPadding(4, 10, 0, 0)

        root.addView(bandTitle)

        // Equalizer sliders

        val bandsScroll = HorizontalScrollView(this)

        val bands = LinearLayout(this)

        bands.orientation = LinearLayout.HORIZONTAL
        bands.gravity = Gravity.CENTER

        frequencies.forEachIndexed { index, frequency ->

            val column = LinearLayout(this)

            column.orientation = LinearLayout.VERTICAL
            column.gravity = Gravity.CENTER

            val valueLabel = TextView(this)

            valueLabel.text = "0 dB"
            valueLabel.textSize = 9f
            valueLabel.gravity = Gravity.CENTER

            valueLabel.setTextColor(Color.LTGRAY)

            val slider = SeekBar(this)

            slider.max = 24
            slider.progress = 12

            slider.rotation = -90f

            slider.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {

                        val db = progress - 12

                        valueLabel.text =
                            "$db dB"

                        if (fromUser) {
                            applyBand(index, db)
                        }
                    }

                    override fun onStartTrackingTouch(
                        seekBar: SeekBar?
                    ) {}

                    override fun onStopTrackingTouch(
                        seekBar: SeekBar?
                    ) {}
                }
            )

            sliders.add(slider)

            column.addView(
                valueLabel,
                LinearLayout.LayoutParams(
                    62,
                    32
                )
            )

            column.addView(
                slider,
                LinearLayout.LayoutParams(
                    62,
                    360
                )
            )

            val frequencyLabel = TextView(this)

            frequencyLabel.text = frequency
            frequencyLabel.textSize = 9f
            frequencyLabel.gravity = Gravity.CENTER

            frequencyLabel.setTextColor(Color.WHITE)

            column.addView(
                frequencyLabel,
                LinearLayout.LayoutParams(
                    62,
                    30
                )
            )

            bands.addView(column)
        }

        bandsScroll.addView(bands)

        root.addView(
            bandsScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                425
            )
        )

        // Bass Boost

        val bassTitle = TextView(this)

        bassTitle.text = "BASS BOOST"
        bassTitle.textSize = 11f
        bassTitle.setTextColor(Color.GRAY)

        root.addView(bassTitle)

        val bassSlider = SeekBar(this)

        bassSlider.max = 1000
        bassSlider.progress = 250

        bassSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    if (fromUser) {

                        try {
                            bassBoost?.setStrength(
                                progress.toShort()
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {}

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {}
            }
        )

        root.addView(bassSlider)

        setContentView(root)
    }

    private fun createAudioEffects() {

        try {

            equalizer =
                Equalizer(0, 0).apply {
                    enabled = true
                }

            bassBoost =
                BassBoost(0, 0).apply {

                    enabled = true

                    setStrength(250)
                }

            try {

                visualizer =
                    Visualizer(0).apply {

                        captureSize =
                            Visualizer
                                .getCaptureSizeRange()[1]

                        setDataCaptureListener(

                            object :
                                Visualizer.OnDataCaptureListener {

                                override fun
                                onWaveFormDataCapture(
                                    visualizer: Visualizer?,
                                    waveform: ByteArray?,
                                    samplingRate: Int
                                ) {
                                }

                                override fun
                                onFftDataCapture(
                                    visualizer: Visualizer?,
                                    fft: ByteArray?,
                                    samplingRate: Int
                                ) {

                                    if (fft != null) {

                                        spectrum.setFft(
                                            fft
                                        )
                                    }
                                }
                            },

                            Visualizer.getMaxCaptureRate() / 2,

                            false,

                            true
                        )

                        enabled = true
                    }

            } catch (_: Exception) {

                spectrum.startFallbackAnimation()
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Audio effects are not available on this device",
                Toast.LENGTH_LONG
            ).show()

            spectrum.startFallbackAnimation()
        }
    }

    private fun applyBand(
        index: Int,
        db: Int
    ) {

        if (!enabled) return

        try {

            val numberOfBands =
                equalizer?.numberOfBands ?: 0

            if (index < numberOfBands) {

                equalizer?.setBandLevel(
                    index.toShort(),
                    (db * 100)
                        .coerceIn(-1200, 1200)
                        .toShort()
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun applyPreset(
        name: String
    ) {

        val values =
            presets[name] ?: return

        values.forEachIndexed { index, db ->

            sliders[index].progress =
                db + 12

            applyBand(
                index,
                db
            )
        }
    }

    override fun onDestroy() {

        try {
            visualizer?.release()
        } catch (_: Exception) {
        }

        try {
            equalizer?.release()
        } catch (_: Exception) {
        }

        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    // Animated Spectrum

    class SpectrumView(
        context: android.content.Context
    ) : View(context) {

        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)

        private var fft =
            ByteArray(0)

        private var fallback =
            false

        private var phase =
            0.0

        init {

            setBackgroundColor(
                Color.rgb(
                    13,
                    15,
                    22
                )
            )
        }

        fun setFft(
            data: ByteArray
        ) {

            fft =
                data.copyOf()

            fallback = false

            postInvalidate()
        }

        fun startFallbackAnimation() {

            fallback = true

            postInvalidate()
        }

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(canvas)

            val barCount = 48

            val gap = 5f

            val barWidth =
                (
                    width -
                        gap *
                        (barCount + 1)
                    ) / barCount

            phase += 0.09

            for (i in 0 until barCount) {

                val level =

                    if (
                        !fallback &&
                        fft.size > 2
                    ) {

                        val position =
                            min(
                                fft.size - 1,
                                2 + i * 2
                            )

                        val magnitude =
                            abs(
                                fft[position].toInt()
                            )

                        (
                            magnitude /
                                128f
                            ).coerceIn(
                                0.08f,
                                1f
                            )

                    } else {

                        (
                            0.15 +
                                0.65 *
                                (
                                    (
                                        sin(
                                            phase +
                                                i * 0.32
                                        ) + 1
                                    ) / 2
                                )
                            ).toFloat()
                    }

                val barHeight =
                    max(
                        8f,
                        height * level
                    )

                val left =
                    gap +
                        i *
                        (
                            barWidth +
                                gap
                        )

                val top =
                    height -
                        barHeight

                paint.color =
                    Color.rgb(
                        0,
                        (
                            170 +
                                70 *
                                level
                            )
                                .toInt()
                                .coerceAtMost(
                                    255
                                ),
                        220
                    )

                canvas.drawRoundRect(
                    left,
                    top,
                    left + barWidth,
                    height.toFloat(),
                    5f,
                    5f,
                    paint
                )
            }

            postInvalidateDelayed(35)
        }
    }
}
