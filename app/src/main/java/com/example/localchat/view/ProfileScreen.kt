package com.example.localchat.view

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.localchat.ui.theme.ctaBlue
import com.example.localchat.ui.theme.greyBackground
import com.example.localchat.ui.theme.lineColor
import com.example.mychat.viewmodel.LoginViewModel
import kotlin.let
import kotlin.text.isNotEmpty


@Composable
fun ProfileScreen(viewModel: LoginViewModel, controller: NavHostController) {
    var name by remember { mutableStateOf("") }
    var selectedImg by remember { mutableStateOf("") }
    var isProcessingImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri->
        uri?.let {
            selectedImg = it.toString()
            Toast.makeText(context, "Image Selected, Tap to change", Toast.LENGTH_SHORT).show()
        }
    }
    Column(
        Modifier.fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier.size(100.dp)
                .background(ctaBlue, shape = RoundedCornerShape(25.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("💬", Modifier, fontSize = 50.sp, textAlign = TextAlign.Center)
        }
        Text(
            "Set up your Profile", Modifier.padding(top = 30.dp, bottom = 10.dp),
            color = Color.Black,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )

        Text(
            "Add a Photo and your name", Modifier.padding(bottom = 32.dp),
            color = Color.Gray, textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.size(120.dp)
            .clip(CircleShape)
            .background(
                color = if(selectedImg!= "") Color.Transparent else lineColor
            )
            .border(width = 2.dp, color  = if(selectedImg!= "") Color.Transparent else lineColor, shape = CircleShape)
            .clickable{
                imagePickerLauncher.launch("image/*")
            },
            contentAlignment = Alignment.Center
        ){
            if(selectedImg != "") {
                Image(
                    painter = rememberAsyncImagePainter(selectedImg), contentDescription = null,
                    Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )


                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Change", Modifier, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }else{
                Column(
                    Modifier, horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📷", Modifier, fontSize = 32.sp)
                    Text("Add Photo", Modifier.padding(top =5.dp), fontSize = 16.sp)
                }
            }
        }
        OutlinedTextField(value = name, onValueChange = {input->
        name = input
        }, Modifier.padding(top =30.dp), placeholder = {
            Text("Enter your name",Modifier, color = Color.Gray)
        },
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent, cursorColor = ctaBlue,
                focusedContainerColor = greyBackground,
                unfocusedContainerColor = greyBackground
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done)

        )
        Button(
            onClick = {
                coroutineScope.launch {
                    isProcessingImage = true
                    viewModel.uploadImage(name, selectedImg, controller)
                    isProcessingImage = false
                }
            },
            Modifier.padding(top = 30.dp), 
            enabled = (name.isNotEmpty() && !isProcessingImage),
            colors = ButtonDefaults.buttonColors(
                containerColor = if(name.isNotEmpty() && !isProcessingImage) ctaBlue else greyBackground
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                if(isProcessingImage) "Processing..." else "Complete Profile", 
                Modifier.padding(horizontal = 50.dp), 
                fontWeight = FontWeight.SemiBold,
                color = if(name.isNotEmpty()) Color.White else Color.Black)
        }
        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = {}
        ) {
            Text("Skip Photo for now!", Modifier,color = ctaBlue)
        }
    }
}