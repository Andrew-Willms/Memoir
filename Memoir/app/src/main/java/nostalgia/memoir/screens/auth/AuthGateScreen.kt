package nostalgia.memoir.screens.auth

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nostalgia.memoir.auth.AuthManager

@Composable
fun AuthGateScreen(onSuccess: () -> Unit) {

    val repo = AuthManager.repo()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(24.dp)) {

        Text("Memoir Login")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(email, { email = it }, label = { Text("Email") })

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(password, { password = it }, label = { Text("Password") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val res = repo.signIn(email, password)
                if (res.isSuccess) onSuccess()
                else error = res.exceptionOrNull()?.message
            }
        }) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            scope.launch {
                val res = repo.signUp(email, password)
                if (res.isSuccess) onSuccess()
                else error = res.exceptionOrNull()?.message
            }
        }) {
            Text("Sign Up")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}