package com.example.localchat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.localchat.ui.theme.ctaBlue
import com.example.localchat.ui.theme.greyBackground
import com.example.mychat.viewmodel.LoginViewModel
import kotlin.text.any
import kotlin.text.isDigit
import kotlin.text.isLowerCase
import kotlin.text.isNotEmpty
import kotlin.text.isUpperCase


@Composable
fun LoginScreen(viewmodel: LoginViewModel, controller: NavHostController) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }
    LaunchedEffect(password, ) {
        enabled = password.length>=8 && password.any {it.isUpperCase()} &&
                password.any {it.isLowerCase()} && password.any {it.isDigit() }
                && password.isNotEmpty()
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
            "Welcome Back!", Modifier.padding(top = 30.dp, bottom = 10.dp),
            color = Color.Black,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )

        Text(
            "Enter you password to login ${viewmodel.email.value}", Modifier.padding(bottom = 32.dp),
            color = Color.Gray, textAlign = TextAlign.Center,
        )
        OutlinedTextField(value = password, onValueChange = {input->
            password = input

        }, Modifier, placeholder = {
            Text("Enter Password",Modifier, color = Color.Gray)
        },
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent, cursorColor = ctaBlue,
                focusedContainerColor = greyBackground,
                unfocusedContainerColor = greyBackground
            ),
            visualTransformation = if(showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(
                    onClick = { showPassword = !showPassword}
                ) {
                    if(showPassword){
                        Text("👁️")
                    }
                    else{
                        Text("👁️‍🗨️")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next)

        )

        Button(
            onClick = {
               viewmodel.login(password, controller)
            },
            Modifier.padding(top = 30.dp), enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if(
                    enabled) ctaBlue else greyBackground
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Submit", Modifier.padding(horizontal = 50.dp), fontWeight = FontWeight.SemiBold,
                color = if(enabled) Color.White else Color.Black)
        }
        Spacer(Modifier.height(24.dp))


        Column(
            Modifier.fillMaxWidth().padding(horizontal = 30.dp).background(greyBackground, shape = RoundedCornerShape(10.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ){
            Text("Password Requirements:", Modifier, fontWeight = FontWeight.SemiBold)
            Text("At Least 8 chars long", Modifier, fontWeight = FontWeight.Normal,
                color = if(password.length>=8 )Color(0xff34c759) else  Color.Gray
            )
            Text("Contains UpperCase Letters", Modifier, fontWeight = FontWeight.Normal,
                color = if(password.any {it.isUpperCase()} )Color(0xff34c759) else  Color.Gray
            )
            Text("Contains LowerCase Letters", Modifier, fontWeight = FontWeight.Normal,
                color = if(password.any {it.isLowerCase()})Color(0xff34c759) else  Color.Gray
            )
            Text("Contains Number", Modifier, fontWeight = FontWeight.Normal,
                color = if(password.any { it.isDigit()} )Color(0xff34c759) else  Color.Gray
            )
        }







    }
}