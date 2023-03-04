package com.example.baneapp2

import android.os.Bundle
import android.util.JsonReader
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.baneapp2.settingstore.StoreUserSettings
import com.example.baneapp2.ui.theme.Baneapp2Theme
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.*
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Random
import kotlin.coroutines.coroutineContext

class MainActivity : ComponentActivity() {


    val okHttpClient = OkHttpClient()
    var websocket: WebSocket? = null
    lateinit var dataBase: DataBase
    lateinit var user: MutableState<User>
    lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBase = Room.databaseBuilder(applicationContext, DataBase::class.java, "db").addTypeConverter(Convert()).build()

        setContent {

            user = remember { mutableStateOf(User(getSharedPreferences("User", MODE_PRIVATE))) }
            Log.e("onCreate", user.value.toString())
            navController = rememberNavController()
            Baneapp2Theme(colors = darkColors()) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (user.value.token != null) "Main" else "Login"
                    ) {
                        composable("Login") { Login() }
                        composable("Register") { Register() }
                        composable("Main") {
                            if (websocket != null) {
                              Main()
                            } else if (connectWebSocket()) {
                                Main()
                            } else {
                                navController.navigate("Login") {
                                    this.popUpTo("Login")
                                }
                            }
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

    override fun onPause() {
        super.onPause()
        user.value.save(getSharedPreferences("User", MODE_PRIVATE));
    }

    @Composable
    fun Settings(){
        var tekstkleurtekst by remember{mutableStateOf("A7A7A7")}
        var achtergrondkleurtekst by remember{mutableStateOf("373737")}
        var voorgrondkleurtekst by remember{mutableStateOf("272727")}
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val dataStore = StoreUserSettings(context)
        val tekstkleur = dataStore.getTextColor.collectAsState(initial = "")
        val achtergrondkleur = dataStore.getBGColor.collectAsState(initial = "")
        val voorgrondkleur = dataStore.getFGColor.collectAsState(initial = "")
        if(tekstkleur.toString() != "") {
            tekstkleurtekst = tekstkleur.toString()
            achtergrondkleurtekst = achtergrondkleur.toString()
            voorgrondkleurtekst = voorgrondkleur.toString()
        }
        var tekstkleurkleur: String = "0xFF" + tekstkleurtekst
        var achtergrondkleurkleur: String = "0xFF"+ achtergrondkleurtekst
        var vooorgrondkleurkleur: String = "0xFF" + voorgrondkleurtekst




        Scaffold(topBar = {
            TopAppBar(
                backgroundColor=Color(vooorgrondkleurkleur.toInt()),
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back to Contacts")
                    }
                },
                title = {
                    Text(text = "Instellingen", color = Color(tekstkleurkleur.toInt()))
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            dataStore.saveBGColor(achtergrondkleurtekst)
                            dataStore.saveFGColor(voorgrondkleurtekst)
                            dataStore.saveTextColor(tekstkleurtekst)

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
                        .background(Color(achtergrondkleurkleur.toInt()))
                        .padding(it)
                ) {
                    Row(modifier = Modifier) {
                        Text(text = "Tekstkleur:", color = Color(tekstkleurkleur.toInt()))
                        SingleLineInput(tekstkleurtekst, {tekstkleurtekst = it})
                    }
                    Row(modifier = Modifier) {
                        Text(text = "Voorgrondkleur:", color = Color(tekstkleurkleur.toInt()))
                        SingleLineInput(voorgrondkleurtekst, {voorgrondkleurtekst = it})
                    }
                    Row(modifier = Modifier) {
                        Text(text = "Achtergrondkleur: ", color = Color(tekstkleurkleur.toInt()))
                        SingleLineInput(achtergrondkleurtekst, {achtergrondkleurtekst = it})
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

            Box(modifier = Modifier
                .padding(it)
                .fillMaxSize()) {
                SingleLineInput(
                    value = name,
                    modifier = Modifier.align(Alignment.Center),
                    onValueChange = {name = it},
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        MainScope().launch(Dispatchers.IO) {
                            val id = addPerson(name)
                            Log.e("AddPerson", id.toString())
                            if (id != null){
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
                })
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
                            Pair(painterResource(R.drawable.subpicture), user.value.name)
                        } else {
                            Pair(painterResource(R.drawable.subpictureother), contact?.name.orEmpty())
                        }
                        val tijddatum =
                            LocalDateTime.ofInstant(messageList[Message].time, ZoneOffset.UTC)
                        val tijd =
                            tijddatum.getHour().toString() + ":" + tijddatum.getMinute().toString()
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

        Scaffold(
            bottomBar = { BottomNavigation() {
                IconButton(onClick = {
                    navController.navigate("AddPerson")
                }) { Icon(Icons.Filled.Add, "Settings") }
                IconButton(onClick = {
                    navController.navigate("Settings")
                }) { Icon(Icons.Filled.Settings, "Settings") }
            } }
        ) {
            Row(modifier = Modifier.padding(it)) {


                LazyColumn(modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)) {
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
        }
    }

    fun sendAndStoreMessage(message: String, id: String) {
        try {
            websocket?.send(
                JSONObject().put("type", "Message").put("message", message).put("receiver", id)
                    .toString()
            )
        } catch (e: Throwable) {
            return Unit
        }
        MainScope().launch {
            dataBase.messageDao().insert(Message(user.value.id, true, message));
        }
    }

    fun connectWebSocket(): Boolean {
        val token = user.value.token
        return if (token != null) {
            websocket = okHttpClient.newWebSocket(
                request = Request.Builder().addHeader("Id", user.value.id).addHeader("Token", token).url("wss://esfokk.nl/api/v0/ws").build(),
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

    fun login(email: String, password: String): Boolean {
            try {
                val body = okHttpClient.newCall(
                    Request.Builder().get()
                        .addHeader("email", email)
                        .addHeader("password", password)
                        .url("https://esfokk.nl/api/v0/login")
                        .build()
                ).execute().body?.string() ?: "{}"
                Log.e("Login", body)
                val json = JSONObject(
                    body
                )
                Log.e("Login", json.toString())
                user.value = User(
                    json.getString("token"),
                    json.getString("name"),
                    json.getString("num"),
                    json.getString("id"),
                    email,
                    json.getString("refresh_token")
                );
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
                        }},
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
            okHttpClient.newCall(Request.Builder().url("https://esfokk.nl/api/v0/register").header("name", name).header("email", email).header("password", password).build()).execute().isSuccessful
        } catch (e: Throwable) {
            false
        }
    }


    inner class WsListener : WebSocketListener() {

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            if (!connectWebSocket()) {
                Log.e("Websocket onClosed", reason.toString())
            }

        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.e("Websocket onOpen", response.toString())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e("Websocket onFailure", t.toString() + ' ' + response?.body.toString())
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
                    Log.e("Websocket onMessage", message.toString())
                    dataBase.messageDao().insert(
                        message
                    )
                } catch (e: Throwable) {
                    Log.e("Websocket onMessage", e.toString())
                }
            }
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

@Preview
@Composable
fun ChatItemPreview() {
    var messagelijsttest = mutableListOf<Message> (Message("hello", true, "woef", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()), Message("boyy", false, "barf", Calendar.getInstance().toInstant()))
    //var testuserinfo = mutableStateOf<>(useri)

    Baneapp2Theme {
        //MessageCard("Dit is een testbericht OwO", "Username", painterResource(R.drawable.subpicture), "4:23")
        //chatu(messagelijsttest)
        Chattest(messagelijsttest)
    }



}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatItem(icon: Painter = painterResource(R.drawable.bane_logo), name: String = "No Name", num: String = "0000", onClick: () -> Unit = {}) {
    Card(onClick = onClick) {
        Row(modifier = Modifier.padding(8.dp)) {
            Image(icon, name, modifier = Modifier.height(30.dp))
            Text(text = name)
            Text(text = "#$num", color = MaterialTheme.colors.primaryVariant)
        }
    }
}
@Composable
fun MessageCard(message: String, name: String,  pfp: Painter , tijd: String) {
    Card(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = MaterialTheme.shapes.medium,
        elevation = 5.dp,
        backgroundColor = Color(0xFF373737)
    ) {
        Column(Modifier.padding(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    pfp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(4.dp),
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
                modifier = Modifier.padding(4.dp)
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
                        textColor = MaterialTheme.colors.background
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

