package com.xgi.jetpack_compose_sample_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.xgi.jetpack_compose_sample_project.ui.theme.ViewModelTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity()
{
	private lateinit var viewModel : CounterViewModel // lateinit variables are assigned after some time elapses post onCreate to make sure that all the UI is initialized first
	
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		
		viewModel = ViewModelProvider(this)[CounterViewModel::class] // Creating a ViewModel object
		var incrementEverySecond : Job? = null // Job variable for concurrency and threading
		
		enableEdgeToEdge()
		setContent {
			ViewModelTheme {
				MainScreen(
					viewModel = viewModel, // Passing the ViewModel object to the compose UI
					onPlay = { // Passing the lambda function to the compose UI to fire whenever an event occur
						val isPlaying = !viewModel.isPlaying.value
						viewModel.updatePlayState(isPlaying)
						incrementEverySecond?.cancel()
						
						if(isPlaying)
						{
							viewModel.setResetVisibility(true)
							incrementEverySecond = lifecycleScope.launch { // Creating new job (cancelled or completed jobs cannot be re executed)
								while(viewModel.counter.value < 10)
								{
									delay(1000)
									viewModel.increment()
								}
								
								viewModel.updatePlayState(false)
							}
						}
					},
					onReset = {
						if(incrementEverySecond == null) return@MainScreen
						
						viewModel.updatePlayState(false)
						viewModel.setCounter()
						viewModel.setResetVisibility(false)
						
						if(incrementEverySecond.isActive) incrementEverySecond.cancel() // Terminating the job
					}
				)
			}
		}
	}
}

class CounterViewModel : ViewModel() // A ViewModel is used to update the UI as the variable in it change
{
	// A mutable state flow emits or broadcasts changes in a variable to the UI
	private val _counter = MutableStateFlow(0)
	private val _isPlaying = MutableStateFlow(false)
	private val _isResetEnabled = MutableStateFlow(false)
	
	// Exposed values
	val counter = _counter.asStateFlow()
	val isPlaying = _isPlaying.asStateFlow()
	val isResetEnabled = _isResetEnabled.asStateFlow()
	
	// Functions that modify the value of MutableStateFlow(s) upon being set from outside the ViewModel
	fun setCounter(value : Int = 0)
	{
		_counter.value = value
	}
	
	fun increment()
	{
		_counter.value++
	}
	
	fun updatePlayState(playState : Boolean)
	{
		_isPlaying.value = playState
	}
	
	fun setResetVisibility(visibility : Boolean)
	{
		_isResetEnabled.value = visibility
	}
}

@Composable
fun MainScreen(
	viewModel : CounterViewModel, // Receiving the ViewModel to listen to the changes in variables
	onReset : () -> Unit = {},
	onPlay : () -> Unit = {}
) {
	// The collectors receives the broadcasts from the ViewModel StateFlow(s) and updates the UI in realtime
	val counterValue by viewModel.counter.collectAsStateWithLifecycle()
	val playState by viewModel.isPlaying.collectAsStateWithLifecycle()
	val resetVisibility by viewModel.isResetEnabled.collectAsStateWithLifecycle()
	
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(text = counterValue.toString(), fontSize = 100.sp)
		Row (
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.Center
		) {
			IconButton(onClick = onPlay) {
				Icon(
					painter = painterResource(id = if(playState) R.drawable.ic_pause else R.drawable.ic_play),
					contentDescription = "Reset"
				)
			}
			
			AnimatedVisibility(resetVisibility) {
				IconButton(onClick = onReset) {
					Icon(
						painter = painterResource(id = R.drawable.ic_reset),
						contentDescription = "Reset"
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
	val previewViewModel = CounterViewModel()
	MainScreen(previewViewModel)
}