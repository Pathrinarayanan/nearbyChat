package com.example.localchat.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.exyte.animatednavbar.animation.indendshape.StraightIndent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.localchat.R
import com.example.mychat.modal.BottomItem
import com.example.mychat.modal.User
import com.example.mychat.viewmodel.LoginViewModel
import com.exyte.animatednavbar.AnimatedNavigationBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LoginViewModel, controller: NavController) {

    var selectedIndex by remember { mutableStateOf(0) }

    val barColor = Color(0xff2C3E50)
    val ballColor = Color(0xff3498DB)
    val contentColor = Color(0xffBDC3C7)
    val activeContentColor = Color(0xffBDC3C7)
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.hasLocationPermission = isGranted
        if (isGranted) {
            viewModel.getCurrentLocation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.hasLocationPermission = viewModel.checkLocationPermission()
        if (!viewModel.hasLocationPermission) {
            viewModel.requestLocationPermission()
        } else {
            viewModel.getCurrentLocation()
        }
        viewModel.loadOtherUsers()
        viewModel.loadRecentChatUsers()
    }

    Scaffold(
        Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Local Chat",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xff2C3E50),
                    titleContentColor = Color.White
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        bottomBar = {
            Column {
                if (selectedIndex == 0) {
                    DistanceFilterCard(viewModel)
                }
                
                AnimatedNavigationBar (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    selectedIndex = selectedIndex,
                    barColor = barColor,
                    ballColor = ballColor,
                    indentAnimation = StraightIndent(
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                   val items = listOf(
                       BottomItem("Nearby", R.drawable.status_icon, R.drawable.status_icon_filled),
                       BottomItem("Chats", R.drawable.chat_icon, R.drawable.chat_icon_filled),
                       BottomItem("Settings", R.drawable.settings_icon, R.drawable.settings_icon_filled),

                   )

                   items.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedIndex = index
                                },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                           Image(
                               painter = painterResource(if(index == selectedIndex) item.selectedImg else item.img),
                               null,
                               modifier = Modifier.size(24.dp),
                               colorFilter = if (index == selectedIndex)
                                   ColorFilter.tint(Color.White)
                               else
                                   null
                           )
                           Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                color = if(index == selectedIndex) activeContentColor else contentColor,
                                fontSize = 12.sp,
                                fontWeight = if(index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal
                            )
                       }
                   }
               }
            }
        }
    ) {


        if (!viewModel.hasLocationPermission) {
            LocationPermissionRequest(
                onRequestPermission = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )
         } else {
             Column(
                 Modifier.fillMaxSize()
                     .padding(it)
                     .background(Color(0xffE5E5E5))
             ) {
                 when (selectedIndex) {
                     0 -> {
                         LazyColumn(
                             Modifier.fillMaxSize()
                                 .padding(horizontal = 20.dp, vertical = 16.dp),
                             verticalArrangement = Arrangement.spacedBy(16.dp)
                         ) {
                             itemsIndexed(viewModel.filteredUsersData) { index, data ->
                                 NearbyChatItem(viewModel, data) {
                                     viewModel.FriendUser = data
                                     controller.navigate("message")
                                 }
                             }
                         }
                     }
                     1 -> {
                         RecentChatsCard(viewModel)
                         
                         LazyColumn(
                             Modifier.fillMaxSize()
                                 .padding(horizontal = 20.dp),
                             verticalArrangement = Arrangement.spacedBy(16.dp)
                         ) {
                             itemsIndexed(viewModel.recentChatUsers) { index, data ->
                                 NearbyChatItem(viewModel, data, true) {
                                     viewModel.FriendUser = data
                                     controller.navigate("message")
                                 }
                             }
                         }
                     }
                     2 -> {
                         SettingsContent(viewModel,controller)
                     }
                 }
             }
         }
    }

}

@Composable
fun RecentChatsCard(viewModel: LoginViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Chats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xff2C3E50)
                )
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Chats Icon",
                    tint = Color(0xff3498DB),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${viewModel.recentChatUsers.size} conversations",
                fontSize = 14.sp,
                color = Color(0xff7F8C8D),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsContent(viewModel: LoginViewModel, controller: NavController) {
    Box (
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Card(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Settings Icon",
                    tint = Color(0xff3498DB),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xff2C3E50)
                )

                Button(
                    onClick = {
                        viewModel.logout(navController = controller)
                    },
                    Modifier.padding(top  = 16.dp)
                ) {
                    Text("Logout", Modifier.padding(4.dp))
                }

            }
        }
    }
}

@Composable
fun DistanceFilterCard(viewModel: LoginViewModel) {
    var sliderValue by remember { mutableStateOf(2.0f) }
    
    Card(
        modifier = Modifier
            .background(Color(0xffE5E5E5))
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distance Filter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xff2C3E50)
                )
                Text(
                    text = formatDistance(sliderValue),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xff3498DB)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    viewModel.updateDistanceFilter(newValue.toDouble())
                },
                valueRange = 0.05f..5.0f,
                steps = 25,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color(0xff3498DB),
                    activeTrackColor = Color(0xff3498DB),
                    inactiveTrackColor = Color(0xffBDC3C7)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "50m",
                    fontSize = 10.sp,
                    color = Color(0xff7F8C8D)
                )
                Text(
                    text = "5km",
                    fontSize = 10.sp,
                    color = Color(0xff7F8C8D)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${viewModel.filteredUsersData.size} users found",
                fontSize = 12.sp,
                color = Color(0xff7F8C8D),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatDistance(km: Float): String {
    return when {
        km < 1.0 -> "${(km * 1000).toInt()}m"
        else -> "${String.format("%.1f", km)}km"
    }
}

@Composable
fun DefaultAvatar() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff4CAF50), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👤",
            fontSize = 24.sp,
            color = Color.White
        )
    }
}

@Composable
fun LocationPermissionRequest(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xffE5E5E5))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Icon",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xff4CAF50)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Location Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "To find nearby users, we need access to your location. This helps us show you people who are close to you.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xff666666)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Allow Location Access",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
fun NearbyChatItem(viewmodel: LoginViewModel, data: User,isFromChat: Boolean?=false, onClick:()->Unit) {
    Row (
        Modifier.fillMaxWidth()
            .clickable{
                onClick()
            }
            .shadow(2.dp,RoundedCornerShape(12.dp))
            .background(
                Color.White, RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Box(Modifier.size(52.dp)){
            if(data.profileImage.isNotEmpty()){
                val image = viewmodel.base64ToBitmap(data.profileImage)
                if(image != null) {
                    Image(
                        painter = rememberAsyncImagePainter(image),
                        contentDescription = "Profile image",
                        Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    DefaultAvatar()
                }
            } else {
                DefaultAvatar()
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(start = 20.dp, top = 10.dp).weight(0.8f)) {
                Text(
                    data?.name ?:"",
                    Modifier,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if(isFromChat==false) {
                    Text(
                        viewmodel.lastSeenMessage(data.lastSeen),
                        Modifier.padding(top = 5.dp),
                        fontSize = 12.sp,
                        color = if (data.isOnline) Color(0xff4CAF50) else Color(0xff828282)
                    )
                }
            }
            if(isFromChat==false) {
                Text(
                    viewmodel.getDistanceToUser(data),
                    Modifier.padding(end = 5.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xff828282)
                )
            }
        }
    }
}