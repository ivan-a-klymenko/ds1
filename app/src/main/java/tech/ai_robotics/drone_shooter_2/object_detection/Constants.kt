package tech.ai_robotics.drone_shooter_2.object_detection

object Constants {
    const val MODEL_PATH = "spot_3x_10x_20x.tflite"
//    пятно над Доктором зло, objects
    const val SPOT_3X_10X_20X = "spot_3x_10x_20x.tflite"
//    фонарь напротив, target
    const val OD6_LANTERN_OPPOSITE_2025_07_12 = "od6_lantern_opposite_2025_07_12.tflite"
//    Mavic на фоне сетки с натянутым фоном на аэродроме
    const val MAVIC_01_06_25 = "mavic_01_06_25.tflite"
//    Mavic на аэродроме на фоне сетки без фона
    const val OD4_MAVIC_P_V2 = "od4_mavic_p_v2.tflite"
//    Navic на аэродроме на фоне неба 50 - 150 м
    const val OD5_DETECTOR_MAVIC_V1 = "od5_detector_mavic_v1.tflite"
    const val OD5_2_MAVIC_AERODROM = "od5_2_mavic_aerodrom.tflite"
    const val LABELS_PATH = "labels.txt"
}

//if (command == 'L') stepperX.moveTo(stepperX.currentPosition() + steps);
//if (command == 'R') stepperX.moveTo(stepperX.currentPosition() - steps);
//if (command == 'T') stepperY.moveTo(stepperY.currentPosition() + steps);
//if (command == 'B') stepperY.moveTo(stepperY.currentPosition() - steps);
//if (command == 'F') activateRelay();
//if (command == 'S') deactivateRelay();
//if (command == 'X') stopStepperX();
//if (command == 'Y') stopStepperY();

