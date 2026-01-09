package com.example.transmit_data

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun Graph(modifier: Modifier = Modifier, currValues: MutableList<Int>) {

    var offsetX by remember { mutableFloatStateOf(0f) }

    Spacer( modifier = Modifier
        .offset(y = 80.dp)
        .fillMaxWidth()
        .height(300.dp)

        .drawBehind {
            val list = currValues.toMutableList()
            val max = (list.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
            val y_size = 600

            for ((index, value) in list.withIndex()) {  //make a copy of the list so that a concurrent modification exception is not thrown
                //Log.d("grapherr", ((value.toFloat()/max)*y_size.toFloat()).toString())
                drawCircle(
                    Color.Red, center = Offset(
                        (index - offsetX.toInt()).dp.toPx(),
                        abs(((value.toFloat()/ max)*y_size.toFloat())-y_size).dp.toPx()
                    ),
                    radius = 2.dp.toPx()
                )
            }

        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            //sayHello()
        }

        .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState {delta ->
                offsetX -= delta
                if (offsetX <= 0.0) {
                    offsetX = (1).toFloat()
                } else if (offsetX >= currValues.size.toFloat()) {
                    offsetX = (currValues.size-1).toFloat()
                }
                Log.d("Graphing", "dragged! $offsetX")
            }
        )
    )
}