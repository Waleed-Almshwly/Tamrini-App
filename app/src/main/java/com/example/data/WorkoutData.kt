package com.example.data

object WorkoutData {
    val beginnerPrograms = listOf(
        WorkoutTable(
            id = "full_body_beginner",
            title = "كامل الجسم للمبتدئين",
            description = "تمارين متكاملة لتنشيط الدورة الدموية، تقوية العضلات الأساسية، وبناء مرونة الجسم بدون أدوات.",
            totalMinutes = 5,
            exercises = listOf(
                Exercise(
                    name = "الهرولة والقفز في المكان",
                    description = "تمرين Jumping Jacks مبسط. حرك يديك وقدميك بانتظام لتنشيط نبضات القلب وتهيئة مفاصل الجسم.",
                    durationSeconds = 30,
                    targetMuscles = "كامل الجسم والقلب",
                    imageType = "jumping_jacks"
                ),
                Exercise(
                    name = "القرفصاء للمبتدئين",
                    description = "Squats خفيف لعضلات الفخذ والمؤخرة. انزل ببطء كأنك تجلس على كرسي وحافظ على استقامة ظهرك ورقبتك.",
                    durationSeconds = 30,
                    targetMuscles = "الأفخاذ والأرداف",
                    imageType = "squat"
                ),
                Exercise(
                    name = "الضغط على الركبتين",
                    description = "Knee Push-ups لتقوية عضلات الصدر والكتف والترايسبس بضغط خفيف ومريح على المفاصل.",
                    durationSeconds = 25,
                    targetMuscles = "الصدر والذراعين",
                    imageType = "pushup"
                ),
                Exercise(
                    name = "تمرين الجسر الألوي",
                    description = "Glute Bridge لرفع مرونة العمود الفقري وتقوية الظهر السفلي والبطن السفلية.",
                    durationSeconds = 30,
                    targetMuscles = "أسفل الظهر والأرداف",
                    imageType = "bridge"
                ),
                Exercise(
                    name = "تمرين اللوح الخشبي الكوع",
                    description = "Full Plank على المرافق لشد كامل عضلات البطن وزيادة التحمل العام. حافظ على توازي جسمك مع الأرض.",
                    durationSeconds = 20,
                    targetMuscles = "عضلات البطن والコア",
                    imageType = "plank"
                )
            )
        ),
        WorkoutTable(
            id = "beginner_core_abs",
            title = "شد وتقوية عضلات البطن",
            description = "برنامج منزلي يركز على شد ترهلات المعدة، صقل منحنيات الخصر، وتقوية عضلات الجذع الحيوية.",
            totalMinutes = 4,
            exercises = listOf(
                Exercise(
                    name = "طحن البطن الكلاسيكي",
                    description = "Crunches لطيفة لعضلات المعدة العلوية. ارفع رأسك وكتفيك لمسافة بسيطة دون ثني الرقبة وتنفّس بعمق.",
                    durationSeconds = 30,
                    targetMuscles = "عضلات البطن العلوية",
                    imageType = "crunch"
                ),
                Exercise(
                    name = "ركلات الفراشة المريحة",
                    description = "Flutter Kicks لتدعيم عضلات البطن السفلى وعضلات الفخذ العلوية. حرك قدميك بضربات صغيرة ثابتة.",
                    durationSeconds = 25,
                    targetMuscles = "عضلات البطن السفلية",
                    imageType = "flutter_kick"
                ),
                Exercise(
                    name = "تمرين طائر الكلب للاتزان",
                    description = "Bird-Dog تمرين حركي مريح لتقوية الظهر والخصر والبطن بالتناوب. حافظ على اتزانك وثبات حركتك.",
                    durationSeconds = 30,
                    targetMuscles = "الظهر السفلي وعضلات الاتزان",
                    imageType = "bird_dog"
                ),
                Exercise(
                    name = "تقريب الركبة إلى الصدر",
                    description = "Knee-to-Chest سحب ركبة بركبة نحو الصدر أثناء الاسترخاء على الظهر لشد دهون الخواصر والمعدة السفلية.",
                    durationSeconds = 30,
                    targetMuscles = "عضلات البطن السفلية والخصر",
                    imageType = "knee_chest"
                ),
                Exercise(
                    name = "تمرين اللوح الجانبي المطور",
                    description = "Modified Side Plank مخصص لشد خواصر البطن والدهون الجانبية. ارتكز على ركبتك وكوعك وارتفع بخصرك.",
                    durationSeconds = 20,
                    targetMuscles = "عضلات الجنب والخصر",
                    imageType = "side_plank"
                )
            )
        ),
        WorkoutTable(
            id = "morning_stretch",
            title = "مرونة وتمليس الصباح",
            description = "امتدادات مرنة وجسدية للتخلص من الكسل الصباحي، تفتيح عضلات الكتف، وفك آلام تيبس أسفل الظهر.",
            totalMinutes = 3,
            exercises = listOf(
                Exercise(
                    name = "وضعية القطة والبقرة",
                    description = "Cat-Cow Stretch لتليين العمود الفقري والرقبة ومساندة تدفق الدم في عضلات الصدر والظهر.",
                    durationSeconds = 40,
                    targetMuscles = "خطوط الظهر والرقبة والمفاصل",
                    imageType = "cat_cow"
                ),
                Exercise(
                    name = "وضعية الطفل المريحة",
                    description = "Child's Pose لتصفيح تمدد الظهر بالكامل واستنشاق الأوكسجين بعمق لتخفيف توترات الأعصاب والتوتر.",
                    durationSeconds = 45,
                    targetMuscles = "مفاصل الفخذ والأكتاف والظهر",
                    imageType = "child_pose"
                ),
                Exercise(
                    name = "امتداد الكوبرا اللطيف",
                    description = "Gentle Cobra لتنشيط الظهر وإرخاء عضلات البطن المنكمشة. ارفع جذعك بمستويات مريحة.",
                    durationSeconds = 30,
                    targetMuscles = "عضلات البطن وأسفل الظهر",
                    imageType = "cobra"
                ),
                Exercise(
                    name = "لف الكتفين الذاتي",
                    description = "Shoulder & Neck Rolls لإزالة ترسبات التعب في الرقبة والكتفين وتهدئة العضلات المجهدة بسلاسة دائرية.",
                    durationSeconds = 30,
                    targetMuscles = "الرقبة والأكتاف والترقوة",
                    imageType = "rolls"
                )
            )
        ),
        WorkoutTable(
            id = "home_cardio_burn",
            title = "حرق السعرات المنزلي",
            description = "كارديو تنفسي خفيف يرفع مستويات اللياقة العامة، ينشط عملية حرق السعرات ويزيد طاقتك اليومية السريعة.",
            totalMinutes = 4,
            exercises = listOf(
                Exercise(
                    name = "المشي السريع بمكانه",
                    description = "High Marching لرفع تدفق الأكسجين وتوليد طاقة بدنية مستدامة بدون حدوث صدمات للمفاصل.",
                    durationSeconds = 35,
                    targetMuscles = "كامل الجسم والساقين",
                    imageType = "marching"
                ),
                Exercise(
                    name = "متسلق الجبال البطيء",
                    description = "Slow Mountain Climber لتنشيط عضلات الكتف والبطن والأرجل. حرك ركبتيك بالتناوب للأمام بنوع من الصبر.",
                    durationSeconds = 30,
                    targetMuscles = "البطون والجزء السفلي",
                    imageType = "climbers"
                ),
                Exercise(
                    name = "تمرين لمس الكاحلين",
                    description = "Ankle Taps لشد عضلات الخصر والبطن الجانبية بفعالية كاملة. استلقِ على ظهرك والمس الكاحل الأيمن ثم الأيسر بالتناوب.",
                    durationSeconds = 30,
                    targetMuscles = "الخواصر والبطن الجانبية",
                    imageType = "taps"
                ),
                Exercise(
                    name = "القفز الهوائي بدون حبل",
                    description = "Air Jumps يحاكي تماماً قفز الحبل الكلاسيكي بمرونة خفيفة على أطراف القدم لنمو حرق الدهون.",
                    durationSeconds = 25,
                    targetMuscles = "بطات الساق والقلب والرئتين",
                    imageType = "air_jumps"
                )
            )
        )
    )
}
