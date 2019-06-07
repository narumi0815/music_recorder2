
package com.example.myapplication

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var playBtn: Button? = null
    private var prevBtn: Button? = null
    private var skipBtn: Button? = null
    private var positionBar: SeekBar? = null
    private var volumeBar: SeekBar? = null
    private var elapsedTimeLabel: TextView? = null
    private var remainingTimeLabel: TextView? = null
    private var mp: MediaPlayer? = null
    private var totalTime: Int = 0
    private var playNumber: Int = 0
    private var playing : Boolean = false
    private var txtMemo : TextView? = null
    // Media Playerの初期化
    private var musicList = mutableListOf(R.raw.music437322, R.raw.music447468, R.raw.music329319, R.raw.music481521, R.raw.music496413)
    var musicListShuffuled : List<Int> = musicList.shuffled(Random(0))
    var musicFile: Int = musicListShuffuled[playNumber]
    var musicId : Int = musicFile - musicListShuffuled.min()!!

    // private val fis = openFileInput("lyric_id_name.txt")

    //（オプション）Warning解消
    private val handler = Handler(Handler.Callback { msg ->
        val currentPosition = msg.what
        // 再生位置を更新
        positionBar!!.progress = currentPosition

        // 経過時間ラベル更新
        val elapsedTime = createTimeLabel(currentPosition)
        elapsedTimeLabel!!.text = elapsedTime

        // 残り時間ラベル更新
        val remainingTime = "- " + createTimeLabel(totalTime - currentPosition)
        remainingTimeLabel!!.text = remainingTime

        true
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playBtn = findViewById(R.id.playBtn)
        prevBtn = findViewById(R.id.prevBtn)
        skipBtn = findViewById(R.id.skipBtn)
        elapsedTimeLabel = findViewById(R.id.elapsedTimeLabel)
        remainingTimeLabel = findViewById(R.id.remainingTimeLabel)
        txtMemo = findViewById(R.id.txtMemo) as TextView

        musicFile = musicListShuffuled[playNumber]
        musicId = musicFile - musicListShuffuled.min()!!

        val fis = openFileInput("title_artist.txt")
        fis.bufferedReader().use { reader ->
            var str = reader.readLine()
            var counter = 0
            while( str != null ) {
                if (counter == musicId) {
                    //Log.d("TAG", "再生したmusicは" + str)
                    txtMemo!!.text = str
                    break
                }
                str = reader.readLine()
                counter++
            }
        }

        mp = MediaPlayer.create(this, musicFile)
        mp!!.isLooping = false
        mp!!.seekTo(0)
        mp!!.setVolume(0.5f, 0.5f)
        totalTime = mp!!.duration

        // 再生位置
        positionBar = findViewById(R.id.positionBar)
        positionBar!!.max = totalTime
        positionBar!!.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mp!!.seekTo(progress)
                        positionBar!!.progress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            }
        )

        // 音量調節
        volumeBar = findViewById(R.id.volumeBar)
        volumeBar!!.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val volumeNum = progress / 100f
                    mp!!.setVolume(volumeNum, volumeNum)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {

                }
            }
        )

        // Thread (positionBar・経過時間ラベル・残り時間ラベルを更新する)
        Thread(Runnable {
            while (mp != null) {
                try {
                    val msg = Message()
                    msg.what = mp!!.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(1000)
                    if (positionBar!!.max == positionBar!!.progress) {

                        //ファイルに鑑賞履歴を記録
                        musicId = musicFile - musicListShuffuled.min()!!
                        val saveData : String = getToday() + "  " + musicId

                        try {
                            // 追記書き込みでオープン (=Context.MODE_APPEND)
                            val outputstream: FileOutputStream = openFileOutput("musicrecode.txt", Context.MODE_APPEND)
                            outputstream.write(saveData.toByteArray())
                            outputstream.write("\n".toByteArray())
                            outputstream.close()
                        } catch (e: IOException) {
                            e.printStackTrace();
                        }

                        playNumber += 1
                        if (playNumber == musicList.count()) {
                            playNumber = 0
                        }

                        musicFile = musicListShuffuled[playNumber]
                        mp = MediaPlayer.create(this, musicFile)
                        mp!!.isLooping = false
                        mp!!.seekTo(0)
                        mp!!.setVolume(0.5f, 0.5f)
                        totalTime = mp!!.duration

                        // 再生位置
                        positionBar = findViewById(R.id.positionBar)
                        positionBar!!.max = totalTime
                        positionBar!!.setOnSeekBarChangeListener(
                            object : SeekBar.OnSeekBarChangeListener {
                                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                    if (fromUser) {
                                        mp!!.seekTo(progress)
                                        positionBar!!.progress = progress
                                    }
                                }

                                override fun onStartTrackingTouch(seekBar: SeekBar) {
                                }

                                override fun onStopTrackingTouch(seekBar: SeekBar) {
                                }
                            }
                        )

                        mp!!.start()

                    }
                } catch (e: InterruptedException) {
                }

            }
        }).start()

    }

    fun createTimeLabel(time: Int): String {
        var timeLabel = ""
        val min = time / 1000 / 60
        val sec = time / 1000 % 60

        timeLabel = "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec

        return timeLabel
    }

    fun playBtnClick(view: View) {
        if (!mp!!.isPlaying) {
            // 停止中
            mp!!.start()
            //Log.d("TAG", "musicリストは" + musicListShuffuled)
            playBtn!!.setBackgroundResource(R.drawable.stop)
            playing = true

        } else {
            // 再生中
            mp!!.pause()
            playBtn!!.setBackgroundResource(R.drawable.play)
            playing = false
        }
    }

    fun skipBtnClick(view: View) {
        if (playing == true) {
            playBtn!!.setBackgroundResource(R.drawable.stop)
        } else {
            playBtn!!.setBackgroundResource(R.drawable.play)
        }
        mp!!.stop()
        mp!!.prepare()


        playNumber += 1
        if (playNumber == musicList.count()) {
            playNumber = 0
        }
        val musicFile: Int = musicListShuffuled[playNumber]
        // Log.d("TAG", "リストの長さは" + i)
        mp = MediaPlayer.create(this, musicFile)
        mp!!.isLooping = false
        mp!!.seekTo(0)
        mp!!.setVolume(0.5f, 0.5f)
        totalTime = mp!!.duration

        // 再生位置
        positionBar = findViewById(R.id.positionBar)
        positionBar!!.max = totalTime
        positionBar!!.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mp!!.seekTo(progress)
                        positionBar!!.progress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            }
        )

        musicId = musicFile - musicListShuffuled.min()!!
        val fis = openFileInput("title_artist.txt")
        fis.bufferedReader().use { reader ->
            var str = reader.readLine()
            var counter = 0
            while( str != null ) {
                if (counter == musicId) {
                    txtMemo!!.text = str
                    break
                }
                str = reader.readLine()
                counter++
            }
        }

        if (playing == true) {
            mp!!.start()
        }

    }

    fun prevBtnClick(view: View) {
        if (playing == true) {
            playBtn!!.setBackgroundResource(R.drawable.stop)
        } else {
            playBtn!!.setBackgroundResource(R.drawable.play)
        }
        mp!!.stop()
        mp!!.prepare()


        playNumber -= 1
        if (playNumber < 0) {
            playNumber = musicList.count() - 1
        }
        val musicFile: Int = musicListShuffuled[playNumber]
        // Log.d("TAG", "リストの長さは" + i)
        mp = MediaPlayer.create(this, musicFile)
        mp!!.isLooping = false
        mp!!.seekTo(0)
        mp!!.setVolume(0.5f, 0.5f)
        totalTime = mp!!.duration

        // 再生位置
        positionBar = findViewById(R.id.positionBar)
        positionBar!!.max = totalTime
        positionBar!!.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mp!!.seekTo(progress)
                        positionBar!!.progress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            }
        )

        musicId = musicFile - musicListShuffuled.min()!!
        val fis = openFileInput("title_artist.txt")
        fis.bufferedReader().use { reader ->
            var str = reader.readLine()
            var counter = 0
            while( str != null ) {
                if (counter == musicId) {
                    txtMemo!!.text = str
                    break
                }
                str = reader.readLine()
                counter++
            }
        }

        if (playing == true) {
            mp!!.start()
        }

    }

    fun getToday(): String {
        val date = Date()
        val format = SimpleDateFormat("yyyy/MM/dd/ HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }
}