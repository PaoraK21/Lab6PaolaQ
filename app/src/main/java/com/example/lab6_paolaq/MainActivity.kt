package com.example.lab6_paolaq

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lab6_paolaq.ui.theme.Lab6_PaolaQTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                // Check for Spanish (Spain)
                if (tts.isLanguageAvailable(Locale("es", "ES")) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = Locale("es", "ES")
                }
                // Check for Spanish (Latin America)
                else if (tts.isLanguageAvailable(Locale("es", "LATAM")) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = Locale("es", "LATAM")
                } else {
                    // Fallback to English if Spanish is not available
                    tts.language = Locale.US
                    Log.e("TTS", "Spanish not available, falling back to English")
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            Lab6_PaolaQTheme {
                var showBingo by remember { mutableStateOf(false) }
                var matrixSize by remember { mutableStateOf(5) }
                var uid by remember { mutableStateOf("") }

                if (showBingo) {
                    BingoScreen(matrixSize, uid, tts) { showBingo = false }
                } else {
                    InitialScreen(
                        onGenerate = { size, generatedUid ->
                            matrixSize = size
                            uid = generatedUid
                            showBingo = true
                        }
                    )
                }
            }
        }
    }
}

data class BingoCell(val number: Int, var isMarked: Boolean = false)

@Composable
fun InitialScreen(onGenerate: (Int, String) -> Unit) {
    var dimensionInput by remember { mutableStateOf("") }
    val uid = remember { generateUID() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Bienvenido a la App de Bingo!")


        Image(
            painter = painterResource(id = R.drawable.bingo),
            contentDescription = null,
            modifier = Modifier.size(300.dp)
        )


        Text(text = "Ingrese la dimensión de la matriz del Bingo")
        OutlinedTextField(
            value = dimensionInput,
            onValueChange = { dimensionInput = it },
            label = { Text("Dimensión") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            try {
                val size = dimensionInput.toInt()
                onGenerate(size, uid)
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Por favor ingrese un número válido", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("GENERAR BINGO")
        }
    }
}

@Composable
fun BingoScreen(matrixSize: Int, uid: String, tts: TextToSpeech, onBack: () -> Unit) {
    var bingoNumbers by remember { mutableStateOf(generateMatrix(matrixSize)) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text("Juego de Bingo - UID: $uid", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until matrixSize) {
            Row(horizontalArrangement = Arrangement.Center) {
                for (j in 0 until matrixSize) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(50.dp)
                            .background(
                                color = if (bingoNumbers[i][j].isMarked) Color.Green else Color(0xFF023E8A),
                                shape = CircleShape
                            )
                            .clickable {
                                if (!bingoNumbers[i][j].isMarked) {
                                    bingoNumbers[i][j] = bingoNumbers[i][j].copy(isMarked = true)
                                    if (checkBingo(bingoNumbers)) {
                                        showBingoDialog(context, tts)
                                        onBack()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bingoNumbers[i][j].number.toString(),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { bingoNumbers = generateMatrix(matrixSize) }) {
            Text("Regenerar Carta")
        }
    }
}

fun generateUID(): String {
    val letters = ('A'..'Z') + ('a'..'z')
    val digits = ('0'..'9')
    return buildString {
        append(letters.random())
        append(letters.random())
        append(digits.random())
        append(digits.random())
        append(letters.random())
    }
}

fun generateMatrix(size: Int): Array<Array<BingoCell>> {
    val numbers = (1..100).shuffled().take(size * size)
    return Array(size) { i -> Array(size) { j -> BingoCell(numbers[i * size + j]) } }
}

fun checkBingo(matrix: Array<Array<BingoCell>>): Boolean {
    val size = matrix.size
    for (i in 0 until size) {
        if (matrix[i].all { it.isMarked } || (0 until size).all { matrix[it][i].isMarked }) return true
    }
    if ((0 until size).all { matrix[it][it].isMarked }) return true
    if ((0 until size).all { matrix[it][size - it - 1].isMarked }) return true
    return false
}

fun showBingoDialog(context: Context, tts: TextToSpeech) {
    AlertDialog.Builder(context)
        .setTitle("¡BINGO!")
        .setMessage("Has ganado el juego de Bingo")
        .setPositiveButton("Aceptar", null)
        .show()
    tts.speak("¡Bingo! Has ganado el juego de Bingo", TextToSpeech.QUEUE_FLUSH, null, "")
}