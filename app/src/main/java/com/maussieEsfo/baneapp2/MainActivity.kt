package com.maussieEsfo.baneapp2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.work.*
import com.maussieEsfo.baneapp2.BuildConfig
import com.maussieEsfo.baneapp2.*
import com.maussieEsfo.baneapp2.R
import com.maussieEsfo.baneapp2.settings.Settings
import com.maussieEsfo.baneapp2.theme.Baneapp2Theme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.time.Duration
import java.util.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    var active = false
    val work = PeriodicWorkRequestBuilder<PollingWorker>(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS).apply {
        if (BuildConfig.DEBUG) {
            setInitialDelay(Duration.ofMillis(0))
        }
        setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
    }.build()
    companion object {
        const val PREF_NAME = "Settings"
        const val WORKER_NAME = "Worker"
    }


    val okHttpClient = OkHttpClient()
    var websocket: WebSocket? = null
    lateinit var dataBase: DataBase
    lateinit var settings: MutableState<Settings>
    lateinit var navController: NavHostController
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        createNotificationChannel()
        dataBase = Room.databaseBuilder(applicationContext, DataBase::class.java, "db").addTypeConverter(
            Convert()
        ).build()
        sharedPreferences = getSharedPreferences(PREF_NAME ,MODE_PRIVATE)
        settings = mutableStateOf(Settings(sharedPreferences))
        setContent {
            navController = rememberNavController()
            Baneapp2Theme(settings.value) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (settings.value.token != null) "Main" else "Login"
                    ) {
                        composable("Login") { Login() }
                        composable("Register") { Register() }
                        composable("Main") {
                            Main()
                        }
                        composable("Settings"){Settings()}
                        composable(
                            "Chat/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) {
                            val id = it.arguments!!.getString("id")!!
                            Chat(id)
                        }
                        composable("AddPerson") {
                            AddPerson()
                        }
                    }
                }
            }
        }
    }
    fun createNotificationChannel() {
        val name = "BaneChannel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(name, name, importance)
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onPause() {
        super.onPause()
        websocket?.close(1000, null)
        settings.value.save(sharedPreferences);
    }

    override fun onStop() {
        super.onStop()
        active = false
    }

    override fun onResume() {
        active = true
        super.onResume()
        connectWebSocket()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(WORKER_NAME, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    @Composable
    fun Settings() {


        val default = darkColors()
        var tekstkleurtekstcheck: String
        tekstkleurtekstcheck = "#" + String.format("%06X", settings.value.text_color.value.toArgb()).takeLast(6)

        var bgkleurtekstcheck: String
        bgkleurtekstcheck =
            "#" + String.format("%06X", settings.value.bg_color.value.toArgb()).takeLast(6)
        var fgkleurtekstcheck: String
        fgkleurtekstcheck = "#" + String.format("%06X", settings.value.fg_color.value.toArgb()).takeLast(6)


        var tekstkleurtekst by remember { mutableStateOf(tekstkleurtekstcheck) }
        var achtergrondkleurtekst by remember { mutableStateOf(bgkleurtekstcheck) }
        var voorgrondkleurtekst by remember { mutableStateOf(fgkleurtekstcheck) }
        var error by remember { mutableStateOf(false) }

        Scaffold(topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back to Contacts")
                    }
                },
                title = {
                    Text(text = "Instellingen")
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            settings.value.bg_color.value = Color(achtergrondkleurtekst.toColorInt())
                            settings.value.fg_color.value = Color(voorgrondkleurtekst.toColorInt())
                            settings.value.text_color.value = Color(tekstkleurtekst.toColorInt())
                            error = false
                        } catch (e: Throwable) {
                            error = true
                        }
                    }) {
                        Icon(Icons.Filled.Save, "Save Changes")
                    }
                }
            )
        }) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                Row(modifier = Modifier) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Tekstkleur:", modifier = Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.body1)
                        Text(text = "Voorgrondkleur:", modifier = Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.body1)
                        Text(text = "Achtergrondkleur: ", modifier = Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.body1)
                    }
                    Column(modifier = Modifier) {
                        SingleLineInput(voorgrondkleurtekst, { voorgrondkleurtekst = it }, label = "Tekstkleur")
                        SingleLineInput(tekstkleurtekst, { tekstkleurtekst = it }, label = "Voorgrondkleur")
                        SingleLineInput(achtergrondkleurtekst, { achtergrondkleurtekst = it }, label = "Achtergrondkleur")
                    }
                }

                Button(
                    onClick = {
                        tekstkleurtekst = "#" + String.format("%06X", Settings.DEFAULT.text_color.value.toArgb()).takeLast(6)
                        achtergrondkleurtekst = "#" + String.format("%06X", Settings.DEFAULT.bg_color.value.toArgb()).takeLast(6)
                        voorgrondkleurtekst = "#" + String.format("%06X", Settings.DEFAULT.fg_color.value.toArgb()).takeLast(6)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(
                            width = TextFieldDefaults.MinWidth,
                            height = TextFieldDefaults.MinHeight
                        )
                        .padding(4.dp)
                ){
                    Text("Change back to defaults", style = MaterialTheme.typography.body1)
                }
                if (error) {
                    Text(text = "Error occured", color = colors.error)
                }
            }


        }
    }

    @Composable
    fun AddPerson() {
        var name by remember { mutableStateOf("") }
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Add Person") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back to Main")
                    }
                }
            )
        }) {

            Box(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                SingleLineInput(
                    value = name,
                    modifier = Modifier.align(Alignment.Center),
                    label = "<name>#<num>",
                    onValueChange = { name = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        MainScope().launch(Dispatchers.IO) {
                            val id = addPerson(name)
                            Log.e("AddPerson", id.toString())
                            if (id != null) {
                                withContext(Dispatchers.Main) {
                                    navController.navigate("Chat/$id")
                                }
                            }
                        }
                    })
                )
            }
        }
    }

    suspend fun addPerson(name: String): String? {
        try {
            val (name_split, num) = name.split('#')
            val id = okHttpClient.newCall(Request.Builder().url("https://esfokk.nl/api/v0/query_name").header("name", name).build()).execute().body?.string()
            Log.e("AddPerson", id.toString())
            return if (id != null && id != "") {
                dataBase.personDao().insert(
                    Person(
                        id,
                        name_split,
                        num,
                        "",
                        false
                    )
                )
                id
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e("addPerson", e.toString())
            return null
        }
    }
    fun updateFavourite(id: String, newFavourite: Boolean) {
        try{
            MainScope().launch(Dispatchers.IO) {
                dataBase.personDao().favourite(id, newFavourite)
            }
        }
        catch (e: Throwable) {
            Log.e("favourite", e.toString())
        }
    }

    @Composable
    fun Chat(id: String) {
        //var contactNaam: String by dataBase.PersonDao()
        val contact by produceState<Person?>(null) {
            value = dataBase.personDao().personById(id)
        }
        val MessageModifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF373737))
            .padding(8.dp)
        val messageList by dataBase.messageDao().messagesById(id).collectAsState(listOf())
        var star by remember { mutableStateOf(Icons.Filled.StarBorder) }
        if(contact?.favourite == true) {
            star = Icons.Filled.Star
        }
        val current = LocalContext.current

        Scaffold(topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back to Contacts")
                    }
                },
                title = {
                    Text(text = contact?.name.orEmpty())
                },
                actions = {
                    IconButton(onClick = {
                        if(star == Icons.Filled.StarBorder) {
                            star = Icons.Filled.Star
                            Toast.makeText(current, "Favourite", Toast.LENGTH_SHORT).show()

                                updateFavourite(id, true)


                        }
                        else{
                            star = Icons.Filled.StarBorder
                            Toast.makeText(current, "Unfavourite", Toast.LENGTH_SHORT).show()

                                updateFavourite(id, false)

                        }
                    }) {
                        Icon(star, "Favourite", modifier = Modifier.size(32.dp))
                    }
                }
            )
        }) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(it)) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    reverseLayout = true
                ) {
                    items(messageList.count()) { Message ->
                        val (pfp, naam) = if (messageList[Message].self) {
                            Pair(painterResource(R.drawable.subpicture), settings.value.name)
                        } else {
                            Pair(painterResource(R.drawable.subpictureother), contact?.name.orEmpty())
                        }
                        val tijddatum =
                            LocalDateTime.ofInstant(messageList[Message].time, ZoneOffset.UTC)
                        val tijd =
                            (tijddatum.getHour()+1).toString() + ":" + tijddatum.getMinute().toString()
                        MessageCard(messageList[Message].message, naam, pfp, tijd)
                    }
                }
                Row {
                    var value by remember { mutableStateOf("") }
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if(value != "") {
                            sendAndStoreMessage(value, id)
                            value = ""
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }

                }
            }
        }
    }


    @Composable
    fun Main() {
        val recent_persons by dataBase.personDao().personsRecently().collectAsState(listOf())
        val favourite_persons by dataBase.personDao().personsFavourite().collectAsState(listOf())

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {Text("Contacts")}
                )
                     },
            bottomBar = {
                BottomNavigation() {
                    IconButton(onClick = {
                        navController.navigate("AddPerson")
                    }) { Icon(Icons.Filled.Add, "Settings", modifier = Modifier.align(Alignment.CenterVertically) .size(32.dp)) }
                    Text(settings.value.name + "#" + settings.value.num, modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.h5)
                    IconButton(onClick = {
                        navController.navigate("Settings")
                    }) { Icon(Icons.Filled.Settings, "Settings", modifier = Modifier.align(Alignment.CenterVertically) .size(32.dp)) }
                }
            }
        ) {
            Row(modifier = Modifier.padding(it)) {
                Column(modifier = Modifier
                    .padding(8.dp)
                ) {
                    Row {
                        Column(modifier = Modifier.weight(1F)) {
                            Row(modifier = Modifier) {
                                Icon(Icons.Filled.AccessTime, "Recent", modifier = Modifier.align(Alignment.CenterVertically) .size(48.dp))
                                Text(text = "Recent", style = MaterialTheme.typography.h5, modifier = Modifier.align(Alignment.CenterVertically))
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                items(recent_persons.count()) { index ->
                                    ChatItem(
                                        name = recent_persons[index].name,
                                        num = recent_persons[index].num,
                                        onClick = {
                                            navController.navigate("Chat/${recent_persons[index].id}")
                                        })
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1F)) {
                            Row(modifier = Modifier) {
                                Icon(Icons.Filled.StarBorder, "Recent",modifier = Modifier.align(Alignment.CenterVertically) .size(48.dp))
                                Text(text = "Favourite", style = MaterialTheme.typography.h5, modifier = Modifier.align(Alignment.CenterVertically))
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                items(favourite_persons.count()) { index ->
                                    ChatItem(
                                        name = favourite_persons[index].name,
                                        num = favourite_persons[index].num,
                                        onClick = {
                                            navController.navigate("Chat/${favourite_persons[index].id}")
                                        })
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    fun sendAndStoreMessage(message: String, id: String) {
        try {
            websocket?.send(
                JSONObject().put("type", "Message").put("message", message).put("receiver", id.toLong())
                    .toString()
            )
        } catch (e: Throwable) {
            Log.e("sendAndStoreMessage", e.toString())
            return Unit
        }
        MainScope().launch {
            dataBase.messageDao().insert(Message(id, true, message));
        }
    }

    fun connectWebSocket(): Boolean {
        val token = settings.value.token
        return if (token != null) {
            val req = Request.Builder().addHeader("id", settings.value.id).addHeader("token", token).url("wss://esfokk.nl/api/v0/ws").build()
            Log.d("Websocket request", req.toString())
            websocket = okHttpClient.newWebSocket(
                request = req,
                listener = WsListener()
            )
            true
        } else {
            false
        }
    }

    @Composable
    fun Login() {
        val focusManager = LocalFocusManager.current
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        Scaffold(topBar = {
            TopAppBar(title = {
                Text(text = "Login")
            })
        }) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painterResource(R.drawable.bane_logo),
                    "Logo",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                SingleLineInput(
                    email,
                    { email = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    label = "Email",
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    })
                )
                PasswordField(
                    password,
                    { password = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onDone = {
                        val handle = MainScope().launch(Dispatchers.IO) {
                            if (login(email, password)) {
                                withContext(Dispatchers.Main) {
                                    navController.navigate("Main")
                                }
                            }
                        }

                    }
                )
                Button(
                    onClick = {
                        MainScope().launch(Dispatchers.IO) {
                            if (login(email, password)) {
                                withContext(Dispatchers.Main) {
                                    navController.navigate("Main")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(
                            width = TextFieldDefaults.MinWidth,
                            height = TextFieldDefaults.MinHeight
                        )
                ) {
                    Text("Login", style = MaterialTheme.typography.h5)
                }
                Button(
                    onClick = {
                        navController.navigate("Register")
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(
                            width = TextFieldDefaults.MinWidth,
                            height = TextFieldDefaults.MinHeight
                        )
                ) {
                    Text("Register", style = MaterialTheme.typography.h5)
                }
            }
        }
    }

    fun login(fun_email: String, password: String): Boolean {
        try {
            val body = okHttpClient.newCall(
                Request.Builder().get()
                    .addHeader("email", fun_email)
                    .addHeader("password", password)
                    .url("https://esfokk.nl/api/v0/login")
                    .build()
            ).execute().body?.string() ?: "{}"
            val json = JSONObject(
                body
            )
            Log.d("Login", json.toString())
            settings.value.apply {
                token = json.getString("token")
                name = json.getString("name")
                num = json.getString("num")
                id = json.getString("id")
                email = fun_email
                refresh_token = json.getString("refresh_token")
            };
        } catch (e: Throwable) {
            Log.e("Login", e.toString())
            return false
        }
        return true

    }

    @Composable
    fun Register() {
        val focusManager = LocalFocusManager.current
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        Scaffold(topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back to Login")
                    }
                },
                title = {
                    Text(text = "Register")
                })
        }) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painterResource(R.drawable.bane_logo),
                    "Logo",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                SingleLineInput(
                    name,
                    { name = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    label = "Name",
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    })
                )
                SingleLineInput(
                    email,
                    { email = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    label = "Email",
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    })
                )
                PasswordField(
                    password,
                    { password = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally),

                    )

                Button(
                    onClick = {
                        MainScope().launch(Dispatchers.IO) {
                            if (register(name, email, password)) {
                                withContext(Dispatchers.Main) {
                                    navController.navigate("Login") {
                                        this.popUpTo("Login")
                                    }
                                }
                            }
                        }
                    },
                    content = { Text("Register", style = MaterialTheme.typography.h5) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(TextFieldDefaults.MinWidth)
                        .height(TextFieldDefaults.MinHeight)
                )
            }
        }
    }

    fun register(name: String, email: String, password: String): Boolean {
        return try {
            okHttpClient.newCall(
                Request.Builder().url("https://esfokk.nl/api/v0/register").header("name", name)
                    .header("email", email).header("password", password).build()
            ).execute().isSuccessful
        } catch (e: Throwable) {
            false
        }
    }


    inner class WsListener : WebSocketListener() {



        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            if (active) {
                connectWebSocket()
            }
            Log.d("Websocket onClosed", reason.toString())
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d("Websocket onOpen", response.toString())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            when (response?.code) {
                410 -> {
                    MainScope().launch(Dispatchers.IO) {
                        refresh_tokens()
                        connectWebSocket()
                    }
                }
                401 -> {
                    settings.value.token = null
                    settings.value.refresh_token = null
                    MainScope().launch(Dispatchers.Main) {
                        navController.navigate("Login")
                    }
                }
                else -> {
                    connectWebSocket()
                    Log.d("Websocket onFailure", t.toString())
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            MainScope().launch(Dispatchers.IO) {
                try {
                    val json = JSONObject(text)
                    val message = Message(
                        sender = json.getString("sender"),
                        self = false,
                        message = json.getString("message")
                    )
                    Log.d("Websocket onMessage", message.toString())
                    dataBase.messageDao().insert(
                        message
                    )
                } catch (e: Throwable) {
                    Log.e("Websocket onMessage", e.toString())
                }
            }
        }
    }

    private suspend fun refresh_tokens() {
        val res = okHttpClient.newCall(Request.Builder().url("https://esfokk.nl/api/v0/refresh").header("refresh_token", settings.value.refresh_token.orEmpty()).header("id", settings.value.id).build()).execute()
        Log.d("refresh_tokens", res.toString())
        if (res.code == 401 || res.code == 410) {
            settings.value.apply {
                token = null
                refresh_token = null
                withContext(Dispatchers.Main) {
                    navController.navigate("Login")
                }
            }
        } else if (res.code == 200) {
            val json = JSONObject(res.body!!.string())
            settings.value.apply {
                token = json.getString("token")
                refresh_token = json.getString("refresh_token")
            }
            settings.value.save(sharedPreferences)
        }
    }
}

@Composable
fun SingleLineInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Input",
    keyboardActions: KeyboardActions = KeyboardActions(),
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        placeholder = { Text(label) },
        modifier = modifier.focusTarget(),
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions
    )
}

@Preview
@Composable
fun PasswordfieldPreview() {
    var value by remember { mutableStateOf("") }
    Baneapp2Theme(Settings()) {
        PasswordField(value, onValueChange = {value = it})
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    onDone: KeyboardActionScope.() -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    TextField(
        value = value,
        label = { Text(label) },
        singleLine = true,
        placeholder = { Text(label) },
        visualTransformation = if (!visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        onValueChange = onValueChange,
        trailingIcon = {
            IconButton({
                visible = !visible
            }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (visible) "Make invisible" else "Make visible",
                )
            }
        },
        modifier = modifier,
        keyboardActions = KeyboardActions(onDone = onDone),
    )
}

//@Preview
//@Composable
//fun ChatItemPreview() {
//    var messagelijsttest = mutableListOf<Message> (Message("hello", true, "woef", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()))
//    //var testuserinfo = mutableStateOf<>(useri)
//
//    Baneapp2Theme {
//        //MessageCard("Dit is een testbericht OwO", "Username", painterResource(R.drawable.subpicture), "4:23")
//        //chatu(messagelijsttest)
//        Chattest(messagelijsttest)
//    }
//
//
//
//}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatItem(icon: Painter = painterResource(R.drawable.bane_logo), name: String = "No Name", num: String = "0000", onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = Modifier.padding(2.dp)) {
        Row(modifier = Modifier.padding(4.dp).fillMaxWidth()) {
            Image(icon, name, modifier = Modifier.height(48.dp))
            Text(text = name)
            Text(text = "#$num", color = colors.primaryVariant)
        }
    }
}
@Composable
fun MessageCard(message: String, name: String,  pfp: Painter , tijd: String) {
    Card(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = MaterialTheme.shapes.medium,
        elevation = 5.dp,
        backgroundColor = Color(0xFF373737)
    ) {
        Column(Modifier.padding(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    pfp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(0.dp),
                    contentScale = ContentScale.Fit,
                )
                Row(Modifier.padding(8.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.h6,

                    )

                        Text(
                            text = tijd,
                            style = MaterialTheme.typography.body2,
                            )

                }

            }
            Text(
                text = message,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
@Composable
fun Chattest(/*navController: NavController, id: */messageList: List<Message>) {
    //var contactNaam: String by dataBase.PersonDao()
    var contactNaam: String = "vriend"
    val MessageModifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF373737))
        .padding(10.dp)

    val textModifier = TextStyle(fontSize = 20.sp, color = Color(0xFFA7A7A7))
    var pfp: Painter
    var eigenNaam: String = "Maurice"
    var naam: String
    var tijd: String
    //val messageList by dataBase.messageDao().messagesById(id).collectAsState(listOf())



    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = {
                    //navController.navigateUp()
                }) {
                    Icon(Icons.Filled.ArrowBack, "Back to Contacts")
                }
            },
            title = {
                Text(text = contactNaam)
            })
    }

    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier
                .weight(10f)
                .fillMaxSize()
                .background(Color(0xFF373737))
                .padding(10.dp)
            ) {
                items(messageList.count()) { Message ->
                    if (messageList[Message].self) {
                        pfp = painterResource(R.drawable.subpicture)
                        naam = eigenNaam
                    } else {
                        pfp = painterResource(R.drawable.subpictureother)
                        naam = contactNaam
                    }
                    var tijddatum =
                        LocalDateTime.ofInstant(messageList[Message].time, ZoneOffset.UTC)
                    tijd = tijddatum.getHour().toString() + ":" + tijddatum.getMinute().toString()
                    MessageCard(messageList[Message].message, naam, pfp, tijd)
                }
            }
            Row(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .weight(1f)

            ) {
                var value by remember { mutableStateOf("") }
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF373737),
                        textColor = colors.background
                    )
                )
                IconButton(onClick = {

                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

