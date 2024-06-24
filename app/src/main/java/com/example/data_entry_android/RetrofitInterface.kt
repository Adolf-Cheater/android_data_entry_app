import retrofit2.Call
import retrofit2.http.GET
import com.example.data_entry_android.model.Entry

interface ApiService {
    @GET("ff08de86-6d3d-40fa-ab10-168043f6c55e")
    fun getAllEntries(): Call<List<Entry>>
}
