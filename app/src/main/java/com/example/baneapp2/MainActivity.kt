package com.example.baneapp2

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.baneapp2.ui.theme.Baneapp2Theme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.last
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar
import java.util.Random
import kotlin.coroutines.coroutineContext

class MainActivity : ComponentActivity() {


    val okHttpClient = OkHttpClient()
    var websocket: WebSocket? = null
    lateinit var dataBase: DataBase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBase = Room.databaseBuilder(applicationContext, DataBase::class.java, "db").addTypeConverter(Convert()).build()

        setContent {

            val userInfo = rememberSaveable { mutableStateOf(User()) }
            val navController = rememberNavController()
            Baneapp2Theme(colors = darkColors()) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (userInfo.value.token != null) "Main" else "Login"
                    ) {
                        composable("Login") { Login(navController) }
                        composable("Register") { Register(navController) }
                        composable("Main") { Main(navController, userInfo) }
                        composable(
                            "Chat/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) {
                            val id = it.arguments!!.getString("id")!!
                            Chat(navController, id)
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun Chat(navController: NavController, id: String) {
        var contactNaam: String = person.toString()
        val MessageModifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF373737))
            .padding(10.dp)
        val textModifier = TextStyle(fontSize = 20.sp, color = Color(0xFFA7A7A7))
        val pfp: Painter = painterResource(R.drawable.subpicture)
        val tijd: String = "4:20"


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
        }) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()

            ) {
                var value by remember { mutableStateOf("") }
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.background,
                        textColor = MaterialTheme.colors.background
                    )
                )
                LazyColumn(modifier = MessageModifier) {
                    items(messages.count()) { Message ->
                        val message = messages[Message]!!
                        MessageCard(message.message, "Motherfricker", pfp, message.time.toString())

                    }
                }
            }
        }
    }


    @Composable
    fun Main(navController: NavController, userInfo: MutableState<User>) {
        val recent_persons by dataBase.personDao().personsRecently().collectAsState(listOf())

        Scaffold(
            bottomBar = { BottomNavigation() {
                IconButton(onClick = { MainScope().launch {
                    val count = Random().nextInt()
                    dataBase.personDao().insert(Person(id = "$count", image = "", name = "person $count", num = count.toString().take(4)))
                } }) { Icon(Icons.Filled.Settings, "Settings") }
            } }
        ) {
            Row(modifier = Modifier.padding(it)) {


                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(recent_persons.count()) { index ->
                        ChatItem(
                            name = recent_persons[index]?.name.orEmpty(),
                            num = recent_persons[index]?.num.orEmpty(),
                            onClick = {
                                navController.navigate("Chat?Id=$index")
                            })
                    }
                }

            }


//
//            BottomAppBar {
//                IconButton(onClick = {navController.navigate("Settings")}){Icon(Icons.Filled.Settings, "Settings")}
//            }
            }
        }



    fun connectWebSocket(userInfo: MutableState<User>): Boolean {
        val token = userInfo.value.token
        if (token != null) {
            websocket = okHttpClient.newWebSocket(
                request = Request.Builder().addHeader("Id", userInfo.value.id).addHeader("Token", token).build(),
                listener = WsListener()
            )
            return true
        } else {
            return false
        }
    }

    @Composable
    fun Login(navController: NavController) {
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
                        if (login(email, password)) {
                            navController.navigate("Main")
                        } else {

                        }
                    }
                )
                Button(
                    onClick = {
                        if (login(email, password)) {
                            navController.navigate("Main")
                        } else{

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
        return true
    }

    @Composable
    fun Register(navController: NavController) {
        val focusManager = LocalFocusManager.current
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        Scaffold(topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
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

    class WsListener : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            val now = Calendar.getInstance()
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
    var messagelijsttest = mutableListOf<Message> (Message("hello", true, "woef", Calendar.getInstance()), Message("boyy", false, "barf", Calendar.getInstance()), Message("boyy", false, "barf", Calendar.getInstance()), Message("boyy", false, "barf", Calendar.getInstance()), Message("boyy", false, "barf", Calendar.getInstance()), Message("boyy", false, "barf", Calendar.getInstance()))
    //var testuserinfo = mutableStateOf<>(useri)

    Baneapp2Theme {
        //MessageCard("Dit is een testbericht OwO", "Username", painterResource(R.drawable.subpicture), "4:23")
        chatu(messagelijsttest)
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
        Column(Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    pfp,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
                Row(Modifier.padding(8.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.h4,

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
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
@Composable
fun chatu(/*navController: NavController, person: Person*/ messages: MutableList<Message>) {
    //var contactNaam: String = person.toString()
    val MessageModifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF373737))
        .padding(10.dp)
    val textModifier = TextStyle(fontSize = 20.sp, color = Color(0xFFA7A7A7))
    var pfp: Painter
    var eigenNaam: String = "Maurice"
    var contactNaam: String = "Era"
    var naam: String
    var tijd: String


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
                Text(text = "contactNaam")
            })
    }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()

        ) {
            var value by remember { mutableStateOf("")}
            TextField(
                value= value,
                onValueChange = {value = it},
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = MaterialTheme.colors.background,
                    textColor = MaterialTheme.colors.background
                )
            )
            LazyColumn(modifier = MessageModifier) {
                items(messages.count()) {Message ->
                    if(messages[Message].self) {
                        pfp = painterResource(R.drawable.subpicture)
                        naam = eigenNaam
                    }
                    else {
                        pfp = painterResource(R.drawable.subpictureother)
                        naam = contactNaam
                    }
                    var tijddatum = messages[Message].time.getTime()
                    tijd = tijddatum.hours.toString() + ":" + tijddatum.minutes.toString()
                    MessageCard(messages[Message].message, naam, pfp, tijd)
                }
            }
        }
    }
}


