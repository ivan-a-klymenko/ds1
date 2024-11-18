package tech.ai_robotics.drone_shooter_2.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import tech.ai_robotics.drone_shooter_2.R
import tech.ai_robotics.drone_shooter_2.bluetooth.DevicesFragment
import tech.ai_robotics.drone_shooter_2.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment(), FragmentManager.OnBackStackChangedListener {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
//        val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
//        requireActivity().actionBar = toolbar


//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.addOnBackStackChangedListener(this)
        if (savedInstanceState == null) childFragmentManager.beginTransaction()
            .add(R.id.fragment, DevicesFragment(), "devices").commit()
        else onBackStackChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onBackStackChanged() {
        activity?.actionBar?.setDisplayHomeAsUpEnabled(childFragmentManager.backStackEntryCount > 0)
    }
}