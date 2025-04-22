package tech.ai_robotics.drone_shooter_2.ui.notifications

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import tech.ai_robotics.drone_shooter_2.databinding.FragmentNotificationsBinding
import tech.ai_robotics.drone_shooter_2.ui.common.Storage

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etZoom.hint = Storage.zoom.toString()
        binding.btSetZoom.setOnClickListener {
            binding.etZoom.text.toString().toFloatOrNull()?.let {
                Toast.makeText(requireActivity(), it.toString(), Toast.LENGTH_SHORT).show()
                Storage.zoom = it
            }
        }
        binding.etTargetVertical.hint = Storage.targetVertical.toString()
        binding.btTargetVertical.setOnClickListener {
            binding.etTargetVertical.text.toString().toDoubleOrNull()?.let {
                Toast.makeText(requireActivity(), it.toString(), Toast.LENGTH_SHORT).show()
                Storage.targetVertical = it
            }
        }
        binding.etTargetHorizontal.hint = Storage.targetHorizontal.toString()
        binding.btTargetHorizontal.setOnClickListener {
            binding.etTargetHorizontal.text.toString().toDoubleOrNull()?.let {
                Toast.makeText(requireActivity(), it.toString(), Toast.LENGTH_SHORT).show()
                Storage.targetHorizontal = it
            }
        }
    }

    private fun testEvent() {
        Log.d("TTT testEvent", System.currentTimeMillis().toString())
        handler.postDelayed(
            {
                Log.d("TTT postDelayed", System.currentTimeMillis().toString())
            },
            500
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }
}