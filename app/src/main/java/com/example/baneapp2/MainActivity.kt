package com.example.baneapp2

import android.app.Service
import android.content.*
import android.os.Bundle
import android.os.IBinder
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
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
import com.example.baneapp2.settings.Settings
import com.example.baneapp2.ui.theme.Baneapp2Theme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.*
import java.time.LocalDateTime
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {

    var websocketService: MutableState<WebsocketService?> = mutableStateOf(null)
    var dataBase: DataBase? = null
    lateinit var navController: NavHostController
    lateinit var settings: Settings
    lateinit var sharedPreferences: SharedPreferences
    private val okHttpClient = OkHttpClient()

    private var connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is WebsocketService.LocalBinder) {
                websocketService.value = service.getServiceClass()
                dataBase = service.getDataBase()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            websocketService.value = null
            dataBase = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        settings = Settings(applicationContext.getSharedPreferences("Settings", MODE_PRIVATE))
        startService(Intent(this, WebsocketService::class.java))
        bindService(
            Intent(this, WebsocketService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        websocketService.value?.connectWebSocket()
        setContent {
            navController = rememberNavController()
            Baneapp2Theme(settings) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (settings.token != null) "Main" else "Login"
                    ) {
                        composable("Login") { Login() }
                        composable("Register") { Register() }
                        composable("Main") {
                            if (websocketService.value != null) {
                                Main()
                            } else {
                                navController.navigate("Login") {
                                    this.popUpTo("Login")
                                }
                            }
                        }
                        composable("Settings") { Settings() }
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
        settings.save(sharedPreferences);
        unbindService(connection)
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, WebsocketService::class.java), connection, BIND_AUTO_CREATE)
    }

    @Composable
    fun Settings() {
        val default = darkColors()
        var tekstkleurtekstcheck: String
        if(settings.text_color.value == null) {
            tekstkleurtekstcheck = "#" + String.format("%06X", Settings.DEFAULT.text_color.value.toArgb()).takeLast(6)
        }
        else {
            tekstkleurtekstcheck = "#" + String.format("%06X", settings.text_color.value.toArgb()).takeLast(6)
        }

        var bgkleurtekstcheck: String
        if(settings.bg_color.value == null) {
            bgkleurtekstcheck = "#" + String.format("%06X", Settings.DEFAULT.bg_color.value.toArgb()).takeLast(6)
        }
        else {
            bgkleurtekstcheck =
                "#" + String.format("%06X", settings.bg_color.value.toArgb()).takeLast(6)
        }
        var fgkleurtekstcheck: String
        if(settings.text_color.value == null) {
            fgkleurtekstcheck = "#" + String.format("%06X", Settings.DEFAULT.fg_color.value.toArgb()).takeLast(6)
        }
        else {
            fgkleurtekstcheck = "#" + String.format("%06X", settings.fg_color.value.toArgb()).takeLast(6)
        }


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
                            settings.bg_color.value = Color(achtergrondkleurtekst.toColorInt())
                            settings.fg_color.value = Color(voorgrondkleurtekst.toColorInt())
                            settings.text_color.value = Color(tekstkleurtekst.toColorInt())
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
                    Text(text = "Tekstkleur:")
                    SingleLineInput(tekstkleurtekst, { tekstkleurtekst = it })
                }
                Row(modifier = Modifier) {
                    Text(text = "Voorgrondkleur:")
                    SingleLineInput(voorgrondkleurtekst, { voorgrondkleurtekst = it })
                }
                Row(modifier = Modifier) {
                    Text(text = "Achtergrondkleur: ")
                    SingleLineInput(achtergrondkleurtekst, { achtergrondkleurtekst = it })
                }
                Button(
                    onClick = {
                        tekstkleurtekst = "#A7A7A7"
                        achtergrondkleurtekst = "#373737"
                        voorgrondkleurtekst = "#171717"
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
                    Text(text = "Error occured", color = MaterialTheme.colors.error)
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


    @Composable
    fun Chat(id: String) {
        //var contactNaam: String by dataBase.PersonDao()
        val contact by produceState<Person?>(null) {
            value = dataBase?.personDao()?.personById(id)
        }
        val MessageModifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF373737))
            .padding(8.dp)
        val messageList = dataBase?.messageDao()?.messagesById(id)?.collectAsState(listOf())?.value



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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    reverseLayout = true
                ) {
                    if (messageList != null) {
                        items(messageList.count()) { Message ->
                            val (pfp, naam) = if (messageList[Message].self) {
                                Pair(painterResource(R.drawable.subpicture), settings.name)
                            } else {
                                Pair(
                                    painterResource(R.drawable.subpictureother),
                                    contact?.name.orEmpty()
                                )
                            }
                            val tijddatum =
                                LocalDateTime.ofInstant(messageList[Message].time, ZoneOffset.UTC)
                            val tijd =
                                tijddatum.getHour().toString() + ":" + tijddatum.getMinute()
                                    .toString()
                            MessageCard(messageList[Message].message, naam, pfp, tijd)
                        }
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
                        if (value != "") {
                            websocketService.value?.sendAndStoreMessage(value, id)
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
        val recent_persons = dataBase?.personDao()?.personsRecently()?.collectAsState(listOf())?.value

        Scaffold(
            bottomBar = {
                BottomNavigation() {
                    IconButton(onClick = {
                        navController.navigate("AddPerson")
                    }) { Icon(Icons.Filled.Add, "Settings") }
                    IconButton(onClick = {
                        navController.navigate("Settings")
                    }) { Icon(Icons.Filled.Settings, "Settings") }
                }
            }
        ) {
            Row(modifier = Modifier.padding(it)) {


                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    if (recent_persons != null) {
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

    fun login(providedEmail: String, password: String): Boolean {
        try {
            val body = okHttpClient.newCall(
                Request.Builder().get()
                    .addHeader("email", providedEmail)
                    .addHeader("password", password)
                    .url("https://esfokk.nl/api/v0/login")
                    .build()
            ).execute().body?.string() ?: "{}"
            Log.e("Login", body)
            val json = JSONObject(
                body
            )
            Log.e("Login", json.toString())
            settings.apply {
                token = json.getString("token")
                name = json.getString("name")
                num = json.getString("num")
                id = json.getString("id")
                email = providedEmail
                refresh_token = json.getString("refresh_token")
            };
            settings.save(getSharedPreferences("Settings", MODE_PRIVATE))
            websocketService.value?.settings = settings
            websocketService.value?.connectWebSocket()
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

    suspend fun addPerson(name: String): String? {
            try {
                val (name_split, num) = name.split('#')
                val id = okHttpClient.newCall(
                    Request.Builder().url("https://esfokk.nl/api/v0/query_name")
                        .header("name", name).build()
                ).execute().body?.string()
                Log.e("AddPerson", id.toString())
                return if (id != null && id != "") {
                    dataBase?.personDao()?.insert(
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

