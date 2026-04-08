package com.mobileide.app.utils

data class FileTemplate(
    val name: String,
    val description: String,
    val fileName: (String) -> String,
    val category: TemplateCategory,
    val generate: (name: String, packageName: String) -> String
)

enum class TemplateCategory(val label: String) {
    ACTIVITY("Activity"),
    COMPOSE("Composable"),
    VIEWMODEL("ViewModel"),
    DATA("Data Layer"),
    TESTING("Testing"),
    UTIL("Utility")
}

object FileTemplates {

    val templates = listOf(

        // ── Compose Screen ─────────────────────────────────────────────────────
        FileTemplate("Composable Screen", "Full screen @Composable with ViewModel", { "$it Screen.kt" }, TemplateCategory.COMPOSE) { name, pkg ->
            """package $pkg

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ${name}Screen(
    onNavigateBack: () -> Unit = {},
    vm: ${name}ViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$name") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    ${name}Content(uiState)
                }
            }
        }
    }
}

@Composable
private fun ${name}Content(uiState: ${name}UiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello from $name!",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}""".trimIndent()
        },

        // ── ViewModel ─────────────────────────────────────────────────────────
        FileTemplate("ViewModel", "ViewModel with StateFlow and UiState", { "${it}ViewModel.kt" }, TemplateCategory.VIEWMODEL) { name, pkg ->
            """package $pkg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ${name}ViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(${name}UiState())
    val uiState: StateFlow<${name}UiState> = _uiState.asStateFlow()

    init {
        load${name}Data()
    }

    fun load${name}Data() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = withContext(Dispatchers.IO) {
                    // TODO: fetch data
                    emptyList<Any>()
                }
                _uiState.update { it.copy(isLoading = false, items = data) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onEvent(event: ${name}Event) {
        when (event) {
            is ${name}Event.Refresh -> load${name}Data()
        }
    }
}

data class ${name}UiState(
    val isLoading: Boolean = false,
    val items: List<Any> = emptyList(),
    val error: String? = null
)

sealed class ${name}Event {
    object Refresh : ${name}Event()
}""".trimIndent()
        },

        // ── Repository ────────────────────────────────────────────────────────
        FileTemplate("Repository", "Repository with interface and implementation", { "${it}Repository.kt" }, TemplateCategory.DATA) { name, pkg ->
            """package $pkg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface ${name}Repository {
    fun getAll(): Flow<List<${name}Model>>
    suspend fun getById(id: Int): ${name}Model?
    suspend fun insert(item: ${name}Model): Long
    suspend fun update(item: ${name}Model)
    suspend fun delete(id: Int)
}

class ${name}RepositoryImpl(
    // private val dao: ${name}Dao,
    // private val api: ApiService
) : ${name}Repository {

    override fun getAll(): Flow<List<${name}Model>> = flow {
        // emit(dao.getAll())
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

    override suspend fun getById(id: Int): ${name}Model? = withContext(Dispatchers.IO) {
        // dao.getById(id)
        null
    }

    override suspend fun insert(item: ${name}Model): Long = withContext(Dispatchers.IO) {
        // dao.insert(item.toEntity())
        0L
    }

    override suspend fun update(item: ${name}Model) = withContext(Dispatchers.IO) {
        // dao.update(item.toEntity())
    }

    override suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        // dao.deleteById(id)
    }
}

data class ${name}Model(
    val id: Int = 0,
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)""".trimIndent()
        },

        // ── UseCase ───────────────────────────────────────────────────────────
        FileTemplate("UseCase", "Clean architecture UseCase", { "Get${it}UseCase.kt" }, TemplateCategory.DATA) { name, pkg ->
            """package $pkg

import kotlinx.coroutines.flow.Flow

class Get${name}UseCase(
    private val repository: ${name}Repository
) {
    operator fun invoke(): Flow<List<${name}Model>> {
        return repository.getAll()
    }
}

class Save${name}UseCase(
    private val repository: ${name}Repository
) {
    suspend operator fun invoke(item: ${name}Model): Result<Long> {
        return try {
            val id = repository.insert(item)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class Delete${name}UseCase(
    private val repository: ${name}Repository
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return try {
            repository.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}""".trimIndent()
        },

        // ── Room Entity + DAO ─────────────────────────────────────────────────
        FileTemplate("Room Entity + DAO", "Room database entity and DAO", { "${it}Entity.kt" }, TemplateCategory.DATA) { name, pkg ->
            """package $pkg

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "${name.lowercase()}s")
data class ${name}Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)

@Dao
interface ${name}Dao {

    @Query("SELECT * FROM ${name.lowercase()}s WHERE is_active = 1 ORDER BY created_at DESC")
    fun getAll(): Flow<List<${name}Entity>>

    @Query("SELECT * FROM ${name.lowercase()}s WHERE id = :id")
    suspend fun getById(id: Int): ${name}Entity?

    @Query("SELECT * FROM ${name.lowercase()}s WHERE name LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<${name}Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ${name}Entity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<${name}Entity>)

    @Update
    suspend fun update(item: ${name}Entity)

    @Delete
    suspend fun delete(item: ${name}Entity)

    @Query("DELETE FROM ${name.lowercase()}s WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM ${name.lowercase()}s")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM ${name.lowercase()}s")
    suspend fun count(): Int
}""".trimIndent()
        },

        // ── Retrofit Service ──────────────────────────────────────────────────
        FileTemplate("API Service", "Retrofit API service interface", { "${it}ApiService.kt" }, TemplateCategory.DATA) { name, pkg ->
            """package $pkg

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ${name}ApiService {

    @GET("${name.lowercase()}s")
    suspend fun getAll(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<${name}ListResponse>

    @GET("${name.lowercase()}s/{id}")
    suspend fun getById(@Path("id") id: Int): Response<${name}Response>

    @POST("${name.lowercase()}s")
    suspend fun create(@Body request: ${name}Request): Response<${name}Response>

    @PUT("${name.lowercase()}s/{id}")
    suspend fun update(
        @Path("id") id: Int,
        @Body request: ${name}Request
    ): Response<${name}Response>

    @DELETE("${name.lowercase()}s/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>

    companion object {
        private const val BASE_URL = "https://api.example.com/v1/"

        fun create(): ${name}ApiService = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(${name}ApiService::class.java)
    }
}

data class ${name}Request(val name: String, val description: String = "")
data class ${name}Response(val id: Int, val name: String, val description: String)
data class ${name}ListResponse(val data: List<${name}Response>, val total: Int, val page: Int)""".trimIndent()
        },

        // ── Unit Test ─────────────────────────────────────────────────────────
        FileTemplate("Unit Test", "ViewModel unit test with coroutines", { "${it}ViewModelTest.kt" }, TemplateCategory.TESTING) { name, pkg ->
            """package $pkg

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ${name}ViewModelTest {

    private lateinit var repository: ${name}Repository
    private lateinit var viewModel: ${name}ViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ${name}ViewModel(/* repository */)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is loading false and empty list`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `loadData sets loading true then false`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isLoading)
            // Trigger load
            viewModel.load${name}Data()
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
        }
    }

    @Test
    fun `error state is set on exception`() = runTest {
        // Configure mock to throw
        // coEvery { repository.getAll() } throws Exception("Network error")

        // viewModel.uiState.test {
        //     val initial = awaitItem()
        //     assertFalse(initial.isLoading)
        //     viewModel.load${name}Data()
        //     val loading = awaitItem()
        //     assertTrue(loading.isLoading)
        //     val error = awaitItem()
        //     assertFalse(error.isLoading)
        //     assertEquals("Network error", error.error)
        // }
    }
}""".trimIndent()
        },

        // ── Broadcast Receiver ────────────────────────────────────────────────
        FileTemplate("BroadcastReceiver", "Android BroadcastReceiver", { "${it}Receiver.kt" }, TemplateCategory.UTIL) { name, pkg ->
            """package $pkg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class ${name}Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Handle boot completed
            }
            ACTION_${name.uppercase()} -> {
                val data = intent.getStringExtra(EXTRA_DATA) ?: return
                // Handle custom action
            }
        }
    }

    companion object {
        const val ACTION_${name.uppercase()} = "$pkg.action.${name.uppercase()}"
        const val EXTRA_DATA = "extra_data"

        fun getIntentFilter() = IntentFilter().apply {
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(ACTION_${name.uppercase()})
        }
    }
}""".trimIndent()
        },
    )

    fun byCategory(cat: TemplateCategory) = templates.filter { it.category == cat }
}
