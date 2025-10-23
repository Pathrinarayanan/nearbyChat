package com.example.mychat.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.mychat.modal.ChatItem
import com.example.mychat.modal.ChatMessage
import com.example.mychat.modal.MessageStatus
import com.example.mychat.modal.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.*
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginViewModel(application: Application): AndroidViewModel(application) {

    val email = mutableStateOf("")
    var FriendUser by mutableStateOf<User?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading  : StateFlow<Boolean> = _isLoading
    val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()
    val firebaseFireStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    var usersData  = mutableStateListOf<User>()
    var filteredUsersData = mutableStateListOf<User>()
    var recentChatUsers = mutableStateListOf<User>()
    val myId = firebaseAuth.currentUser?.uid
    var messageListener : ListenerRegistration ?=null
    
    // Location related properties
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    var currentLocation by mutableStateOf<Location?>(null)
    var hasLocationPermission by mutableStateOf(false)
    var isLocationPermissionRequested by mutableStateOf(false)
    var maxDistanceKm by mutableStateOf(2.0) // Default 2km, can be changed by user

    fun login(password: String, controller: NavHostController){
        val context = getApplication<Application>().applicationContext
        firebaseAuth.signInWithEmailAndPassword(email.value, password)
            .addOnSuccessListener {
                Toast.makeText(context, "Login Successful", Toast.LENGTH_LONG).show()
                controller.navigate("chat")
            }
            .addOnFailureListener {
                Toast.makeText(context, "Login Failed, ${it.message}", Toast.LENGTH_LONG).show()

            }
    }

    fun registerUser(password: String, controller: NavHostController){
        _isLoading.value = true
        val context = getApplication<Application>().applicationContext
        try {
            firebaseAuth.createUserWithEmailAndPassword(email.value, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(context, "User Registered", Toast.LENGTH_LONG).show()
                        val user = firebaseAuth.currentUser
                        val userData = hashMapOf(
                            "uid" to user?.uid,
                            "email" to user?.email,
                            "name" to "",
                            "createdAt" to System.currentTimeMillis()
                        )
                        firebaseFireStore.collection("users").document(user!!.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                controller.navigate("profile")
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, it.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                    } else {
                        Toast.makeText(context, "error in user registration", Toast.LENGTH_LONG)
                            .show()

                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
        }catch (e : Exception){
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
        finally {
            _isLoading.value = false
        }
    }

    fun checkEmailExists(email :String , onResult : (Boolean)->Unit){
        firebaseFireStore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener {
                val exists = !it.isEmpty
                onResult(exists)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
    fun loadOtherUsers(){
        val context = getApplication<Application>().applicationContext
        val currentUid = firebaseAuth.currentUser?.uid?: return
        firebaseFireStore.collection("users").whereNotEqualTo("uid", currentUid)
            .addSnapshotListener { result , _ ->
                usersData.clear()
                usersData.addAll( result?.documents?.mapNotNull {it->
                   val user =  it.toObject(User::class.java)
                    user?.copy(isOnline =   it.getBoolean("isOnline") ?: false)

                }
                    ?: emptyList())

                usersData.find {
                    it.uid == FriendUser?.uid
                }.let {user->
                    FriendUser = user
                }
                
                filterUsersByLocation()
            }

    }
    
    // Location permission and services methods
    fun checkLocationPermission(): Boolean {
        val context = getApplication<Application>().applicationContext
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestLocationPermission() {
        hasLocationPermission = checkLocationPermission()
        isLocationPermissionRequested = true
    }
    
    fun getCurrentLocation() {
        if (!hasLocationPermission) return
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = location
                        // Always update location in database when screen opens
                        updateUserLocation(location.latitude, location.longitude)
                        filterUsersByLocation()
                    } else {
                        // If lastLocation is null, try to get fresh location
                        requestFreshLocation()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(getApplication(), "Failed to get location", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(getApplication(), "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestFreshLocation() {
        try {
            // Request fresh location if lastLocation is null
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    updateUserLocation(location.latitude, location.longitude)
                    filterUsersByLocation()
                }
            }.addOnFailureListener {
                Toast.makeText(getApplication(), "Failed to get fresh location", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(getApplication(), "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUserLocation(latitude: Double, longitude: Double) {
        val currentUid = firebaseAuth.currentUser?.uid ?: return
        val userMap = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
        firebaseFireStore.collection("users").document(currentUid)
            .update(userMap as Map<String, Any>)
            .addOnFailureListener {
                Toast.makeText(getApplication(), "Failed to update location", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun filterUsersByLocation() {
        if (currentLocation == null) {
            filteredUsersData.clear()
            filteredUsersData.addAll(usersData)
            return
        }
        
        val nearbyUsers = usersData.filter { user ->
            if (user.latitude == 0.0 && user.longitude == 0.0) {
                false // Skip users without location data
            } else {
                val distance = calculateDistance(
                    currentLocation!!.latitude,
                    currentLocation!!.longitude,
                    user.latitude,
                    user.longitude
                )
                distance <= maxDistanceKm
            }
        }
        
        filteredUsersData.clear()
        filteredUsersData.addAll(nearbyUsers)
    }
    
    fun updateDistanceFilter(distanceKm: Double) {
        maxDistanceKm = distanceKm
        filterUsersByLocation()
    }
    
    fun loadRecentChatUsers() {
        val currentUid = firebaseAuth.currentUser?.uid ?: return
        
        // First try to get recent chats from user document
        firebaseFireStore.collection("users")
            .document(currentUid)
            .get()
            .addOnSuccessListener { document ->
                val recentChatIds = document.get("recentChats") as? List<String>
                
                if (!recentChatIds.isNullOrEmpty()) {
                    println("DEBUG: Found ${recentChatIds.size} recent chat IDs")
                    
                    // Get user details for recent chat participants
                    val batches = recentChatIds.chunked(10)
                    val allUsers = mutableListOf<User>()
                    var completedBatches = 0
                    
                    batches.forEach { batch ->
                        firebaseFireStore.collection("users")
                            .whereIn("uid", batch)
                            .get()
                            .addOnSuccessListener { userResult ->
                                println("DEBUG: Found ${userResult.documents.size} users in batch")
                                allUsers.addAll(userResult.toObjects(User::class.java))
                                completedBatches++
                                
                                // When all batches are complete, update the UI
                                if (completedBatches == batches.size) {
                                    println("DEBUG: Final user count: ${allUsers.size}")
                                    recentChatUsers.clear()
                                    recentChatUsers.addAll(allUsers)
                                }
                            }
                            .addOnFailureListener { exception ->
                                println("DEBUG: Error fetching users: ${exception.message}")
                                completedBatches++
                                if (completedBatches == batches.size) {
                                    recentChatUsers.clear()
                                    recentChatUsers.addAll(allUsers)
                                }
                            }
                    }
                } else {
                    println("DEBUG: No recent chats found, falling back to chat document method")
                    loadRecentChatUsersFromChats()
                }
            }
            .addOnFailureListener { exception ->
                println("DEBUG: Error fetching user document: ${exception.message}")
                loadRecentChatUsersFromChats()
            }
    }
    
    private fun loadRecentChatUsersFromChats() {
        val currentUid = firebaseAuth.currentUser?.uid ?: return
        
        // Fallback: Get all chat documents where current user is involved
        firebaseFireStore.collection("chats")
            .get()
            .addOnSuccessListener { result ->
                val chatUserIds = mutableSetOf<String>()
                
                println("DEBUG: Found ${result.documents.size} chat documents")
                
                result.documents.forEach { chatDoc ->
                    val chatId = chatDoc.id
                    val userIds = chatId.split("_")
                    if (userIds.size == 2) {
                        val user1 = userIds[0]
                        val user2 = userIds[1]
                        
                        // Add the other participant (not current user)
                        if (user1 == currentUid) {
                            chatUserIds.add(user2)
                        } else if (user2 == currentUid) {
                            chatUserIds.add(user1)
                        }
                    }
                }
                
                println("DEBUG: Found ${chatUserIds.size} unique chat users")
                
                // Get user details for each chat participant
                if (chatUserIds.isNotEmpty()) {
                    val batches = chatUserIds.chunked(10)
                    val allUsers = mutableListOf<User>()
                    var completedBatches = 0
                    
                    batches.forEach { batch ->
                        firebaseFireStore.collection("users")
                            .whereIn("uid", batch)
                            .get()
                            .addOnSuccessListener { userResult ->
                                allUsers.addAll(userResult.toObjects(User::class.java))
                                completedBatches++
                                
                                if (completedBatches == batches.size) {
                                    recentChatUsers.clear()
                                    recentChatUsers.addAll(allUsers)
                                }
                            }
                            .addOnFailureListener {
                                completedBatches++
                                if (completedBatches == batches.size) {
                                    recentChatUsers.clear()
                                    recentChatUsers.addAll(allUsers)
                                }
                            }
                    }
                } else {
                    recentChatUsers.clear()
                }
            }
            .addOnFailureListener { exception ->
                println("DEBUG: Error fetching chats: ${exception.message}")
                recentChatUsers.clear()
            }
    }
    
    fun getDistanceToUser(user: User): String {
        if (currentLocation == null || user.latitude == 0.0 && user.longitude == 0.0) {
            return "Unknown"
        }
        
        val distance = calculateDistance(
            currentLocation!!.latitude,
            currentLocation!!.longitude,
            user.latitude,
            user.longitude
        )
        
        return if (distance < 1.0) {
            "${(distance * 1000).toInt()} M"
        } else {
            "${String.format("%.1f", distance)} KM"
        }
    }
    fun base64ToBitmap(base64 : String ): Bitmap?{
        return try{
            if(base64.isEmpty()) return null
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }catch (e: Exception ){
            null
        }
    }
    
    fun getDefaultProfileBitmap(): Bitmap {
        // Create a simple default profile bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4CAF50")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(50f, 50f, 50f, paint)
        
        // Add a simple icon
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("👤", 50f, 60f, textPaint)
        
        return bitmap
    }
    fun groupMessage(message: List<ChatMessage>) : List<ChatItem>{
       val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
       val dayFormat = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
        return message
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .flatMap { (date, msg)->
                val header = when(date){
                    dateFormat.format(System.currentTimeMillis()) ->"Today"
                    dateFormat.format(System.currentTimeMillis() - 24*60*60*1000) ->"Yesterday"
                    else-> dayFormat.format(Date(msg.first().timestamp))
                }
                msg.map {
                    ChatItem.Message(it)
                }+ listOf(ChatItem.Header(header))
            }
    }

    fun  sendMessage(senderId : String , receiverId : String, message : String){
        val chatMessage = ChatMessage(senderId, receiverId, message)
        val chatId = if (senderId < receiverId ) "${senderId}_$receiverId" else "${receiverId}_$senderId"
        firebaseFireStore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(chatMessage)
            .addOnSuccessListener {
                // Update recent chats for both users
                updateRecentChats(senderId, receiverId)
                updateRecentChats(receiverId, senderId)
            }
    }
    
    private fun updateRecentChats(userId: String, chatUserId: String) {
        firebaseFireStore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val recentChats = document.get("recentChats") as? MutableList<String> ?: mutableListOf()
                
                // Remove if already exists and add to front
                recentChats.remove(chatUserId)
                recentChats.add(0, chatUserId)
                
                // Keep only last 20 recent chats
                if (recentChats.size > 20) {
                    recentChats.removeAt(recentChats.size - 1)
                }
                
                // Update the document
                firebaseFireStore.collection("users")
                    .document(userId)
                    .update("recentChats", recentChats)
                loadRecentChatUsers()
            }
    }
    fun startListeningMesssage(senderId: String, receiverId : String,onMessageChanged: (List<ChatMessage>) ->Unit){
        messageListener = listenMessages(senderId, receiverId, onMessageChanged)
    }
    fun stopListeningMessage(){
        messageListener?.remove()
        messageListener = null
    }
    fun verifyMail( controller: NavHostController) {
        viewModelScope.launch {
            checkEmailExists(email.value) { exists ->
                if (exists) {
                    // go to login page
                    controller.navigate("login")
                } else {
                    controller.navigate("password")
                }
            }
        }
    }


    fun listenMessages(
        senderId : String , receiverId : String,
        onMessageChanged: (List<ChatMessage>) ->Unit
    ) : ListenerRegistration{
        val chatId = if (senderId < receiverId ) "${senderId}_$receiverId" else "${receiverId}_$senderId"
        return firebaseFireStore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapShot, _->
                val messages = snapShot?.toObjects(ChatMessage::class.java) ?: emptyList()
                //receiver recived and render on his screen,
                snapShot?.documents?.forEach {doc->
                   val user = doc.toObject(ChatMessage::class.java)
                    if(user?.receiverId == myId){
                        doc.reference.update("status", MessageStatus.READ)
                    }

                }

                onMessageChanged(messages)
            }
    }



    fun uriToBase64(context: Context, uri: Uri): String {
        return try {
            // First, get image dimensions without loading full image
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            // Calculate sample size to reduce memory usage
            val sampleSize = calculateInSampleSize(options, 400, 400)
            
            // Load compressed image
            val compressedInputStream = context.contentResolver.openInputStream(uri)
            val compressedOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            
            val bitmap = BitmapFactory.decodeStream(compressedInputStream, null, compressedOptions)
            compressedInputStream?.close()
            
            if (bitmap == null) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                return ""
            }
            
            // Compress bitmap to JPEG with quality 80%
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()
            
            // Check size limit (2MB instead of 9MB)
            if (compressedBytes.size > 2_000_000) {
                Toast.makeText(context, "Image too large, please choose a smaller image", Toast.LENGTH_LONG).show()
                bitmap.recycle()
                return ""
            }
            
            bitmap.recycle()
            Base64.encodeToString(compressedBytes, Base64.DEFAULT)
            
        } catch (e: OutOfMemoryError) {
            Toast.makeText(context, "Image too large, please choose a smaller image", Toast.LENGTH_LONG).show()
            ""
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        } catch (e: Exception) {
            Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    fun uploadImage(name : String, url : String , controller: NavController){
        val context = getApplication<Application>().applicationContext
        val uuid = firebaseAuth.currentUser?.uid ?:return

        val base64Image = uriToBase64(context, url.toUri())
        
        // If image processing failed, base64Image will be empty
        if(base64Image.isEmpty()){
            return
        }
        
        val userMap = hashMapOf(
            "uid" to uuid,
            "email" to email.value,
            "name" to name,
            "profileImage" to base64Image,
            "createdAt" to System.currentTimeMillis()
        )
        firebaseFireStore.collection("users").document(uuid)
            .set(userMap)
            .addOnSuccessListener {
                controller.navigate("chat")
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG)
                    .show()
                    }
    }
    fun lastSeenMessage(timeStamp : Long) : String{
        val diff = System.currentTimeMillis() - timeStamp
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        when {
            timeStamp == 0L ->  return "online"
            diff < 60 * 1000  -> return "last seen few seconds ago"
            diff < 60 * 60 * 1000  -> {
                val min = diff/ (60 * 60 * 1000)
                return "last seen $min ago"
            }
            diff < 24 * 60 *60  * 1000 ->{
                return "last seen today at ${sdf.format(Date(timeStamp))}"
            }
            diff < 2 * 24 * 60 *60  * 1000 ->{
                return "last seen yesterday at ${sdf.format(Date(timeStamp))}"
            }

        }
        val sdfDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return "last seen on ${sdfDate.format(Date(timeStamp))}"
    }
    fun messageTimeStamp(time: Long): String{
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(time))
    }
    fun logout(navController: NavController){
        firebaseAuth.signOut()
        navController.navigate("email") {
            popUpTo(0) // clears back stack
        }
    }
}