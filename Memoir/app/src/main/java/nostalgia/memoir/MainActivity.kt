package nostalgia.memoir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import nostalgia.memoir.diagnostics.DatabaseSelfTestResult
import nostalgia.memoir.diagnostics.DatabaseSelfTestRunner
import nostalgia.memoir.diagnostics.DatabaseSelfTestSuite
import nostalgia.memoir.ui.theme.MemoirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoirTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (BuildConfig.DEBUG) {
                        DatabaseSelfTestScreen(
                            runner = DatabaseSelfTestRunner(applicationContext),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    } else {
                        ReleaseHomePlaceholder(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DatabaseSelfTestScreen(
    runner: DatabaseSelfTestRunner,
    modifier: Modifier = Modifier,
) {
    var isRunning by remember { mutableStateOf(true) }
    var suites by remember { mutableStateOf<List<DatabaseSelfTestSuite>>(emptyList()) }

    LaunchedEffect(Unit) {
        suites = runner.runAllSuites()
        isRunning = false
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Memoir Database Self-Tests",
            fontWeight = FontWeight.Bold,
        )

        if (isRunning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                Text("Running tests...")
            }
        } else {
            val results = suites.flatMap { it.results }
            val passedCount = results.count { it.passed }
            Text("$passedCount/${results.size} tests passed")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suites.forEach { suite ->
                    item {
                        Text(
                            text = suite.name,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(suite.results) { result ->
                        TestResultRow(result = result)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DatabaseSelfTestPreview() {
    MemoirTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Memoir Database Self-Tests", fontWeight = FontWeight.Bold)
            TestResultRow(
                result = DatabaseSelfTestResult(
                    name = "Create entry aggregate",
                    passed = true,
                    details = "Passed",
                ),
            )
            TestResultRow(
                result = DatabaseSelfTestResult(
                    name = "Search entries",
                    passed = false,
                    details = "Expected 1 match, found 0",
                ),
            )
        }
    }
}

@Composable
private fun ReleaseHomePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Memoir")
        Text(text = "Release build: diagnostics screen disabled")
    }
}

@Composable
private fun TestResultRow(result: DatabaseSelfTestResult) {
    val marker = if (result.passed) "✅" else "❌"
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = "$marker ${result.name}")
        Text(text = result.details)
    }
}