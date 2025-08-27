package tech.ai_robotics.drone_shooter_2.object_detection

object Constants {
    const val MODEL_PATH = "spot_3x_10x_20x.tflite"
//    пятно над Доктором зло, objects
    const val SPOT_3X_10X_20X = "spot_3x_10x_20x.tflite"
//    Mavic на фоне сетки с натянутым фоном на аэродроме
    const val MAVIC_01_06_25 = "mavic_01_06_25.tflite"
//    Mavic на аэродроме на фоне сетки без фона
    const val OD4_MAVIC_P_V2 = "od4_mavic_p_v2.tflite"
//    Navic на аэродроме на фоне неба 50 - 150 м
    const val OD5_DETECTOR_MAVIC_V1 = "od5_detector_mavic_v1.tflite"
    const val OD5_2_MAVIC_AERODROM = "od5_2_mavic_aerodrom.tflite"
//    фонарь напротив, target
    const val OD6_LANTERN_OPPOSITE_2025_07_12 = "od6_lantern_opposite_2025_07_12.tflite"
//    Mavic на airsoft полигоне
    const val OD7_2_MAVIC_AIRSOFT_POLIGONO = "od7_2_mavic_airsoft_poligono.tflite"
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

//vPoints: [Point(currentTimeMillis=1753541809560, timestamp=2025-07-26 16:56:49.560, value=0.19051203), Point(currentTimeMillis=1753541809911, timestamp=2025-07-26 16:56:49.911, value=0.2154355), Point(currentTimeMillis=1753541810270, timestamp=2025-07-26 16:56:50.270, value=0.23884457), Point(currentTimeMillis=1753541810626, timestamp=2025-07-26 16:56:50.626, value=0.25774214), Point(currentTimeMillis=1753541810985, timestamp=2025-07-26 16:56:50.985, value=0.27731568), Point(currentTimeMillis=1753541811344, timestamp=2025-07-26 16:56:51.344, value=0.29495978), Point(currentTimeMillis=1753541811703, timestamp=2025-07-26 16:56:51.703, value=0.31079403), Point(currentTimeMillis=1753541812054, timestamp=2025-07-26 16:56:52.054, value=0.32402003), Point(currentTimeMillis=1753541812410, timestamp=2025-07-26 16:56:52.410, value=0.34063208), Point(currentTimeMillis=1753541812766, timestamp=2025-07-26 16:56:52.766, value=0.35301858), Point(currentTimeMillis=1753541813119, timestamp=2025-07-26 16:56:53.119, value=0.3632192), Point(currentTimeMillis=1753541813475, timestamp=2025-07-26 16:56:53.475, value=0.37266612), Point(currentTimeMillis=1753541813829, timestamp=2025-07-26 16:56:53.829, value=0.3825984), Point(currentTimeMillis=1753541814175, timestamp=2025-07-26 16:56:54.175, value=0.39111316), Point(currentTimeMillis=1753541814520, timestamp=2025-07-26 16:56:54.520, value=0.39874956), Point(currentTimeMillis=1753541814867, timestamp=2025-07-26 16:56:54.867, value=0.40519813), Point(currentTimeMillis=1753541815219, timestamp=2025-07-26 16:56:55.219, value=0.41288897), Point(currentTimeMillis=1753541815577, timestamp=2025-07-26 16:56:55.577, value=0.41817242), Point(currentTimeMillis=1753541816267, timestamp=2025-07-26 16:56:56.267, value=0.4295407), Point(currentTimeMillis=1753541816616, timestamp=2025-07-26 16:56:56.616, value=0.4352549)]
//vPoints: [Point(currentTimeMillis=1753541816267, timestamp=2025-07-26 16:56:56.267, value=0.4295407), Point(currentTimeMillis=1753541816616, timestamp=2025-07-26 16:56:56.616, value=0.4352549), Point(currentTimeMillis=1753541817322, timestamp=2025-07-26 16:56:57.322, value=0.444714), Point(currentTimeMillis=1753541838714, timestamp=2025-07-26 16:57:18.714, value=0.34535164), Point(currentTimeMillis=1753541847468, timestamp=2025-07-26 16:57:27.468, value=0.10723275), Point(currentTimeMillis=1753541848180, timestamp=2025-07-26 16:57:28.180, value=0.054986373), Point(currentTimeMillis=1753541848888, timestamp=2025-07-26 16:57:28.888, value=0.011355973), Point(currentTimeMillis=1753541883082, timestamp=2025-07-26 16:58:03.082, value=0.43534064), Point(currentTimeMillis=1753541897290, timestamp=2025-07-26 16:58:17.290, value=0.43103808), Point(currentTimeMillis=1753541897638, timestamp=2025-07-26 16:58:17.638, value=0.41909108), Point(currentTimeMillis=1753541897984, timestamp=2025-07-26 16:58:17.984, value=0.40614653), Point(currentTimeMillis=1753541899053, timestamp=2025-07-26 16:58:19.053, value=0.3556108), Point(currentTimeMillis=1753541899412, timestamp=2025-07-26 16:58:19.412, value=0.33746096), Point(currentTimeMillis=1753541899765, timestamp=2025-07-26 16:58:19.765, value=0.31819683), Point(currentTimeMillis=1753541900994, timestamp=2025-07-26 16:58:20.994, value=0.2577836), Point(currentTimeMillis=1753541901354, timestamp=2025-07-26 16:58:21.354, value=0.23935102), Point(currentTimeMillis=1753541901706, timestamp=2025-07-26 16:58:21.706, value=0.21581194), Point(currentTimeMillis=1753541902406, timestamp=2025-07-26 16:58:22.406, value=0.18320723), Point(currentTimeMillis=1753541902758, timestamp=2025-07-26 16:58:22.758, value=0.16879621), Point(currentTimeMillis=1753541903457, timestamp=2025-07-26 16:58:23.457, value=0.13942017)]
//50 m: hSize: 0.030749321
//100 m: hSize: 0.015184432