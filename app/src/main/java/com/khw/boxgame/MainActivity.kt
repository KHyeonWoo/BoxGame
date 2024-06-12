package com.khw.boxgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khw.boxgame.ui.theme.BoxGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoxGameTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {

    //문제의 난이도를 결정하는 칸 개수 선언
    var length by remember {
        mutableIntStateOf(4)
    }
    //게임을 초기화 하기위한 변수 선언
    var newGame: Int by remember() {
        mutableIntStateOf(0)
    }

    //length, newGame이 바뀔 때마다 새로운 문제가 나오도록 키값 설정
    //paintedList - 문제 리스트 (drawPicture 함수는 총 칸의 갯수의 30프로를 false로 만들어줌)
    //checkedList - 사용자가 체크한 리스트
    val paintedList by remember(length, newGame) { mutableStateOf(drawPicture(length)) }
    val checkedList by remember(length, newGame) {
        mutableStateOf(Array(length) {
            Array(length) {
                mutableStateOf(
                    false
                )
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "시작 버튼을 눌러주세요",
            style = TextStyle(fontSize = 40.sp),
            modifier = Modifier.padding(top = 8.dp),
            color = Color.Blue
        )
        Row(modifier = Modifier.padding(top = 4.dp)) {
            for (i in 3..8) {
                Button(onClick = {
                    length = i
                }) {
                    Text(text = i.toString())
                }
            }
        }

        val stopWatch = remember(length) { StopWatch() }

        var name: String by remember {
            mutableStateOf("이름없음")
        }
        Row {

            StopWatchDisplay(
                formattedTime = stopWatch.formattedTime,
                onStartClick = stopWatch::start,
                onResetClick = stopWatch::reset,
                onNewGame = {
                    newGame++
                    stopWatch.reset()
                }
            )

            TextField(
                value = name,
                onValueChange = { newName -> name = newName },
                label = { Text("이름") },
                modifier = Modifier
                    .width(120.dp)
                    .padding(start = 20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                ),
                textStyle = TextStyle(color = Color.Black)
            )
        }
        val context = LocalContext.current
        val db = remember {
            AppDatabase.getDatabase(context)
        }

        NemoGame(
            paintedList, length, checkedList, stopWatch, name, db, newGame
        )

    }
}

@Composable
fun StopWatchDisplay(
    formattedTime: String,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row {
            Button(
                onClick = {
                    onNewGame()
                    onStartClick()
                },
                colors = ButtonDefaults.buttonColors(Color.Blue)

            ) {
                Text("시작", color = Color.White)
            }
            Button(
                onClick = {
                    onNewGame()
                    onResetClick()
                }) {
                Text("종료")
            }

        }

        Text(
            text = formattedTime,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = Color.Black
        )
    }
}

class StopWatch {

    var formattedTime by mutableStateOf("00:00:000")
    private var coroutineScope = CoroutineScope(Dispatchers.Main)
    var isActive = false

    private var timeMillis = 0L
    private var lastTimestamp = 0L

    fun start() {
        if (isActive) return

        coroutineScope.launch {
            lastTimestamp = System.currentTimeMillis()
            this@StopWatch.isActive = true
            while (this@StopWatch.isActive) {
                delay(10L)
                timeMillis += System.currentTimeMillis() - lastTimestamp
                lastTimestamp = System.currentTimeMillis()
                formattedTime = formatTime(timeMillis)
            }
        }
    }

    fun pause() {
        isActive = false
    }

    fun reset() {
        coroutineScope.cancel()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        timeMillis = 0L
        lastTimestamp = 0L
        formattedTime = "00:00:000"
        isActive = false
    }

    private fun formatTime(timeMillis: Long): String {
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timeMillis),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatter.ofPattern(
            "mm:ss:SSS",
            Locale.getDefault()
        )
        return localDateTime.format(formatter)
    }
}

fun drawPicture(length: Int): Array<Array<Boolean>> {
    val paintedList = Array(length) { Array(length) { true } }

    for (i in 0..length * length / 3) {
        val x = (0 until length).random()
        val y = (0 until length).random()
        paintedList[x][y] = false
    }

    return paintedList
}

@Composable
fun NemoGame(
    paintedList: Array<Array<Boolean>>,
    length: Int,
    checkedList: Array<Array<MutableState<Boolean>>>,
    stopWatch: StopWatch,
    name: String,
    db: AppDatabase,
    newGame: Int,

    ) {

    val paintedCnt = Array(length * 2) { 0 }

    for (i in 0 until length) {
        var widthCnt: Int = 0
        var heightCnt: Int = 0
        for (j in 0 until length) {

            if (paintedList[i][j]) {
                widthCnt++
                if (j == length - 1) {
                    paintedCnt[i] = paintedCnt[i] * 10 + widthCnt
                }
            } else if (widthCnt != 0) {
                paintedCnt[i] = paintedCnt[i] * 10 + widthCnt
                widthCnt = 0
            }

            if (paintedList[j][i]) {
                heightCnt++
                if (j == length - 1) {
                    paintedCnt[i + length] = paintedCnt[i + length] * 10 + heightCnt
                }
            } else if (heightCnt != 0) {
                paintedCnt[i + length] = paintedCnt[i + length] * 10 + heightCnt
                heightCnt = 0
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            for (i in length until paintedCnt.size) {
                Text(
                    text = "${paintedCnt[i]}", modifier = Modifier
                        .padding(20.dp)
                        .width(8.dp)
                )
            }
        }
        for (col in 0 until length) {

            Row {
                Text(
                    text = "${paintedCnt[col]}",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 4.dp),
                    fontSize = 12.sp
                )
                for (row in 0 until length) {

                    Button(
                        onClick = {
                            checkedList[col][row].value = !checkedList[col][row].value
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color.Black),
                        shape = RectangleShape,
                        colors = if (checkedList[col][row].value) {
                            ButtonDefaults.buttonColors(containerColor = Color.Black)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = Color.White)
                        }
                    ) {
                    }
                }
            }
        }

        var result: Int? by remember(length, newGame) {
            mutableStateOf(0)
        }

        var cnt: Int by remember(length) {
            mutableIntStateOf(0)
        }
        val rememberCoroutineScope = rememberCoroutineScope()
        val isStart: Boolean = stopWatch.isActive
        var time: String by remember() {
            mutableStateOf("")
        }
        Button(
            enabled = isStart,
            onClick = {
                for (col in 0 until length) {
                    for (row in 0 until length) {
                        if (checkedList[col][row].value != paintedList[col][row]) {
                            result = 2
                            cnt++
                            break
                        } else {
                            result = 1
                        }
                    }
                    if (result == 2) {
                        break
                    }
                }

                if (result == 1) {

                    stopWatch.pause()
                    time = stopWatch.formattedTime
                    rememberCoroutineScope.launch(Dispatchers.IO) {
                        db.userDao().insertAll(
                            User(
                                name = name,
                                length = length,
                                time = time,
                                count = cnt
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .padding(top = 20.dp)
                .align(Alignment.End)
        ) {
            Text(text = "완료", fontSize = 20.sp)
        }
        if (result == 1) {
            Text(
                text = "실패 횟수: $cnt",
                color = Color.Red,
                fontSize = 24.sp,
                modifier = Modifier.align(alignment = Alignment.End)
            )
            Text(
                text = "걸린 시간: ${time}",
                color = Color.Red,
                fontSize = 24.sp,
                modifier = Modifier.align(alignment = Alignment.End)
            )

        } else if (result == 2) {
            Text(text = "실패", color = Color.Red, fontSize = 40.sp)
        }

        if (!isStart) {
            Text(text = "연습모드", color = Color.Red, fontSize = 40.sp)


            val userList by db.userDao().getRank(length).collectAsState(initial = emptyList())
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                for (i in userList.indices) {
                    Text(text = "${i + 1}등 : ${userList[i].name} - ${userList[i].time}")
                }
            }
        }

    }
}

