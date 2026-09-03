package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcfradera.shooterranking.R

@Composable
fun HomeScreen(
    onCoach: () -> Unit,
    onPlayer: () -> Unit
) {

    CenteredScaffold(
        title = null,

        /*
         * HomeScreen es la ÚNICA pantalla
         * donde mostramos el botón
         * de Configuración.
         */
        showSettings = true
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        vertical = 12.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Image(
                painter =
                    painterResource(
                        R.drawable.titulo_sr
                    ),

                contentDescription =
                    stringResource(
                        R.string.app_name
                    ),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            16f / 7f
                        )
                        .clip(
                            RoundedCornerShape(
                                20.dp
                            )
                        ),

                contentScale =
                    ContentScale.Crop
            )

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string.home_choose_mode
                    ),

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.SemiBold,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            Button(
                onClick = onCoach,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            58.dp
                        )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string.home_coach
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Button(
                onClick = onPlayer,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            58.dp
                        )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string.home_player
                        )
                )
            }
        }
    }
}