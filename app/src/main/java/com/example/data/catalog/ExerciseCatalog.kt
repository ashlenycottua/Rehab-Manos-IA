package com.example.data.catalog

import com.example.data.model.EbookChapter
import com.example.data.model.ExerciseCategory
import com.example.data.model.HandMotionType
import com.example.data.model.MotionPhase
import com.example.data.model.PrimaryGoal
import com.example.data.model.RehabExercise

object ExerciseCatalog {

    val allExercises: List<RehabExercise> = listOf(
        // 1. MOVILIDAD ARTICULAR
        RehabExercise(
            id = "wrist_flex_ext",
            title = "Flexión y extensión de muñeca",
            category = ExerciseCategory.MOVILIDAD,
            motionType = HandMotionType.WRIST_FLEX_EXT,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 10,
            suggestedMinutes = 4,
            summary = "Movilización progresiva de la muñeca apoyando el antebrazo sobre una mesa para liberar tensión articular.",
            postureInstructions = listOf(
                "Siéntate cómodo con la espalda recta y apoya el antebrazo sobre una mesa acolchada.",
                "Deja la mano sobresaliendo suavemente del borde de la mesa con la palma hacia abajo.",
                "Eleva el dorso de la mano despacio hacia arriba sin despegar el antebrazo.",
                "Desciende luego la mano con control sin forzar el límite si aparece tirantez intensa."
            ),
            phases = listOf(
                MotionPhase("Elevar muñeca", "Eleva suavemente la mano hacia arriba", 3, 1f),
                MotionPhase("Sostener", "Mantén la postura con los hombros relajados", 2, 1f),
                MotionPhase("Bajar suave", "Desciende la mano despacio y relaja", 3, 0f)
            ),
            avoidWarnings = listOf(
                "Evita levantar el codo o compensar con el hombro.",
                "No realices rebotes bruscos al llegar al punto máximo.",
                "Detén el ejercicio si sientes pinchazo agudo, hormigueo o mareos."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOVILIDAD, PrimaryGoal.RECUPERACION_SUAVE),
            visionTargetPoseName = "Extensión suave de muñeca con dedos relajados"
        ),
        RehabExercise(
            id = "tabletop_bend",
            title = "Flexión en mesa (posición de techo)",
            category = ExerciseCategory.MOVILIDAD,
            motionType = HandMotionType.TABLETOP_FLEX,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 10,
            suggestedMinutes = 4,
            summary = "Flexión exclusiva de los nudillos manteniendo los dedos estirados en forma de L, ideal para rigidez articular.",
            postureInstructions = listOf(
                "Apoya el codo sobre la mesa con el antebrazo vertical y la mano apuntando hacia el techo.",
                "Mantén la muñeca neutra y alineada con el antebrazo.",
                "Dobla solo los nudillos principales formando un ángulo recto, con los dedos rectos como una visera."
            ),
            phases = listOf(
                MotionPhase("Doblar nudillos", "Dobla solo los nudillos manteniendo dedos rectos", 3, 1f),
                MotionPhase("Sostener", "Sostén la posición de mesa durante dos segundos", 2, 1f),
                MotionPhase("Extender", "Regresa despacio a la mano abierta vertical", 3, 0f)
            ),
            avoidWarnings = listOf(
                "No enrosques las puntas de los dedos durante este movimiento.",
                "Evita inclinar la muñeca hacia adelante o hacia los costados."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOVILIDAD, PrimaryGoal.RECUPERACION_SUAVE),
            visionTargetPoseName = "Mano en posición de mesa (nudillos flexionados a 90 grados)"
        ),
        RehabExercise(
            id = "wrist_radial_glide",
            title = "Inclinación lateral de muñeca",
            category = ExerciseCategory.MOVILIDAD,
            motionType = HandMotionType.WRIST_FLEX_EXT,
            difficultyLabel = "Suave",
            suggestedSets = 2,
            suggestedReps = 10,
            suggestedMinutes = 3,
            summary = "Deslizamiento lateral suave de la mano sobre la mesa en gesto de saludo para ganar amplitud lateral.",
            postureInstructions = listOf(
                "Apoya la palma plana sobre una superficie lisa o toalla.",
                "Mantén el antebrazo quieto y desliza la mano hacia el lado del pulgar.",
                "Vuelve al centro y desliza suavemente hacia el lado del meñique."
            ),
            phases = listOf(
                MotionPhase("Hacia el pulgar", "Desliza suavemente hacia el lado del pulgar", 3, 0.85f),
                MotionPhase("Centro y meñique", "Pasa por el centro y desliza hacia el meñique", 3, 0.25f),
                MotionPhase("Reposo neutro", "Vuelve al centro y descansa", 2, 0f)
            ),
            avoidWarnings = listOf(
                "No gires el antebrazo ni despegues la palma de la mesa.",
                "Evita forzar el movimiento si hay inflamación reciente."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOVILIDAD),
            visionTargetPoseName = "Mano abierta apoyada con alineación neutra de muñeca"
        ),

        // 2. FORTALECIMIENTO
        RehabExercise(
            id = "soft_grip_squeeze",
            title = "Apretón suave de pelota blanda",
            category = ExerciseCategory.FORTALECIMIENTO,
            motionType = HandMotionType.GRIP_SQUEEZE,
            difficultyLabel = "Moderado",
            suggestedSets = 3,
            suggestedReps = 8,
            suggestedMinutes = 4,
            summary = "Activación controlada de los flexores de la mano utilizando una pelota de espuma muy blanda o una toalla enrollada.",
            postureInstructions = listOf(
                "Sostén una pelota blanda o toalla pequeña en la palma de tu mano.",
                "Mantén la muñeca recta, nunca caída hacia abajo.",
                "Presiona de forma uniforme y progresiva hasta un esfuerzo cómodo del 50%."
            ),
            phases = listOf(
                MotionPhase("Presionar suave", "Aprieta la pelota de forma lenta y uniforme", 3, 1f),
                MotionPhase("Mantener", "Sostén la presión sin contener la respiración", 3, 1f),
                MotionPhase("Soltar lento", "Abre los dedos despacio y relaja por completo", 3, 0f)
            ),
            avoidWarnings = listOf(
                "No uses pelotas duras si estás en etapa temprana de recuperación.",
                "No aprietes hasta sentir dolor agudo o temblor."
            ),
            recommendedForGoals = listOf(PrimaryGoal.FUERZA),
            visionTargetPoseName = "Puño suave y simétrico con muñeca recta"
        ),
        RehabExercise(
            id = "isometric_pinch",
            title = "Pinza firme con esponja",
            category = ExerciseCategory.FORTALECIMIENTO,
            motionType = HandMotionType.PINCH_CONTROL,
            difficultyLabel = "Moderado",
            suggestedSets = 3,
            suggestedReps = 8,
            suggestedMinutes = 4,
            summary = "Fortalece la pinza lateral y terminal entre el pulgar y el índice para acciones cotidianas como girar llaves o abrir envases.",
            postureInstructions = listOf(
                "Coloca una esponja o almohadilla blanda entre la yema del pulgar y el índice.",
                "Forma un círculo redondeado con ambos dedos antes de presionar.",
                "Presiona suavemente sin colapsar la articulación de la punta del pulgar."
            ),
            phases = listOf(
                MotionPhase("Presionar pinza", "Presiona suavemente formando una letra O", 3, 1f),
                MotionPhase("Sostener", "Mantén el contacto firme sin tensar el cuello", 2, 1f),
                MotionPhase("Relajar", "Separa los dedos despacio y descansa", 2, 0f)
            ),
            avoidWarnings = listOf(
                "Evita que la punta del pulgar se doble hacia atrás en hiperextensión.",
                "Detente si notas molestia en la base del pulgar."
            ),
            recommendedForGoals = listOf(PrimaryGoal.FUERZA, PrimaryGoal.MOTRICIDAD_FINA),
            visionTargetPoseName = "Pinza redondeada en forma de O entre pulgar e índice"
        ),
        RehabExercise(
            id = "finger_extension_band",
            title = "Apertura contra resistencia leve",
            category = ExerciseCategory.FORTALECIMIENTO,
            motionType = HandMotionType.FINGER_SPREAD,
            difficultyLabel = "Moderado",
            suggestedSets = 2,
            suggestedReps = 10,
            suggestedMinutes = 3,
            summary = "Equilibra la musculatura extensora abriendo los dedos en abanico con una banda elástica suave o contra el aire.",
            postureInstructions = listOf(
                "Junta las yemas de todos los dedos como un capullo.",
                "Si tu profesional lo indicó, rodea los dedos con una banda elástica suave.",
                "Abre los dedos en abanico separándolos de manera pareja."
            ),
            phases = listOf(
                MotionPhase("Abrir abanico", "Separa los dedos de forma pareja hacia afuera", 3, 1f),
                MotionPhase("Sostener", "Mantén los dedos separados dos segundos", 2, 1f),
                MotionPhase("Cerrar lento", "Vuelve a juntar las yemas sin golpear", 3, 0f)
            ),
            avoidWarnings = listOf(
                "No uses bandas demasiado tensas que doblen la muñeca.",
                "Realiza el regreso de forma controlada."
            ),
            recommendedForGoals = listOf(PrimaryGoal.FUERZA, PrimaryGoal.MOVILIDAD),
            visionTargetPoseName = "Mano abierta con dedos separados en abanico"
        ),

        // 3. MOTRICIDAD FINA
        RehabExercise(
            id = "thumb_opposition_o",
            title = "Oposición del pulgar dedo a dedo",
            category = ExerciseCategory.MOTRICIDAD_FINA,
            motionType = HandMotionType.THUMB_OPPOSITION,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 8,
            suggestedMinutes = 4,
            summary = "Secuencia precisa en la que el pulgar toca suavemente la yema de cada dedo formando un círculo limpio.",
            postureInstructions = listOf(
                "Apoya el codo cómodo sobre la mesa con la palma mirando hacia ti.",
                "Lleva el pulgar a tocar la yema del índice formando una O redonda.",
                "Continúa con el dedo medio, anular y meñique en orden y sin apuro."
            ),
            phases = listOf(
                MotionPhase("Tocar dedo", "Une la yema del pulgar con el siguiente dedo", 2, 1f),
                MotionPhase("Abrir mano", "Extiende suavemente los dedos entre cada toque", 2, 0f)
            ),
            avoidWarnings = listOf(
                "Evita aplastar los dedos en pinza plana; busca redondear el arco.",
                "No fuerces el alcance al meñique si hay tirantez en la palma."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOTRICIDAD_FINA, PrimaryGoal.RECUPERACION_SUAVE),
            visionTargetPoseName = "Oposición precisa pulgar-dedo formando círculo"
        ),
        RehabExercise(
            id = "pinch_lift_precision",
            title = "Traslado de precisión con pinza",
            category = ExerciseCategory.MOTRICIDAD_FINA,
            motionType = HandMotionType.PINCH_CONTROL,
            difficultyLabel = "Suave",
            suggestedSets = 2,
            suggestedReps = 10,
            suggestedMinutes = 4,
            summary = "Práctica de coordinación óculo-manual tomando objetos pequeños y ligeros (como tapas o fichas) de un lado a otro.",
            postureInstructions = listOf(
                "Coloca cinco objetos livianos sobre la mesa frente a ti.",
                "Toma uno con la yema del pulgar e índice sin encoger el hombro.",
                "Trasládalo suavemente unos centímetros y suéltalo con control."
            ),
            phases = listOf(
                MotionPhase("Tomar con pinza", "Sujeta el objeto ligero con pulgar e índice", 3, 1f),
                MotionPhase("Soltar suave", "Deposita y abre la pinza lentamente", 3, 0f)
            ),
            avoidWarnings = listOf(
                "No aprietes el objeto con fuerza excesiva.",
                "Mantén la muñeca estable sin dejarla caer sobre la mesa."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOTRICIDAD_FINA),
            visionTargetPoseName = "Pinza fina controlada de pulgar e índice"
        ),
        RehabExercise(
            id = "finger_tapping_piano",
            title = "Tecleo rítmico tipo piano",
            category = ExerciseCategory.MOTRICIDAD_FINA,
            motionType = HandMotionType.FINGER_SPREAD,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 10,
            suggestedMinutes = 3,
            summary = "Elevación y descenso individual de cada dedo sobre la mesa para despertar la independencia y coordinación.",
            postureInstructions = listOf(
                "Apoya la palma ligeramente arqueada sobre la mesa, como si tocaras un piano.",
                "Levanta un solo dedo a la vez sin arrastrar los demás.",
                "Baja el dedo con suavidad antes de pasar al siguiente."
            ),
            phases = listOf(
                MotionPhase("Elevar dedo", "Eleva un dedo manteniendo los demás apoyados", 2, 1f),
                MotionPhase("Apoyar suave", "Desciende suavemente sobre la yema", 2, 0f)
            ),
            avoidWarnings = listOf(
                "No golpees la mesa con fuerza.",
                "Evita tensar la muñeca mientras mueves el anular o el meñique."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOTRICIDAD_FINA, PrimaryGoal.MOVILIDAD),
            visionTargetPoseName = "Mano arqueada sobre superficie con dedos alineados"
        ),

        // 4. MOVILIDAD DE DEDOS
        RehabExercise(
            id = "tendon_gliding_sequence",
            title = "Deslizamiento tendinoso guiado",
            category = ExerciseCategory.MOVILIDAD_DEDOS,
            motionType = HandMotionType.TENDON_GLIDE,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 8,
            suggestedMinutes = 5,
            summary = "Secuencia clásica de mano recta a gancho y puño plano que favorece el deslizamiento suave de los tendones flexores.",
            postureInstructions = listOf(
                "Comienza con la mano abierta, muñeca neutra y dedos juntos apuntando arriba.",
                "Pasa a posición de gancho doblando solo las puntas de los dedos.",
                "Continúa hacia puño plano sin apretar y regresa a mano abierta."
            ),
            phases = listOf(
                MotionPhase("Mano recta", "Extiende suavemente todos los dedos hacia arriba", 2, 0f),
                MotionPhase("Posición gancho", "Dobla solo las articulaciones medias y puntas", 3, 0.55f),
                MotionPhase("Puño suave", "Cierra en puño plano sin apretar", 3, 1f)
            ),
            avoidWarnings = listOf(
                "Realiza cada transición de forma lenta y continua, sin tirones.",
                "No aprietes el puño con fuerza al cerrar."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOVILIDAD, PrimaryGoal.RECUPERACION_SUAVE),
            visionTargetPoseName = "Transición suave entre mano abierta y puño relajado"
        ),
        RehabExercise(
            id = "hook_fist_glide",
            title = "Garra suave (puño en gancho)",
            category = ExerciseCategory.MOVILIDAD_DEDOS,
            motionType = HandMotionType.HOOK_FIST,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 10,
            suggestedMinutes = 3,
            summary = "Movimiento focalizado en las articulaciones medias y distales de los dedos para reducir la rigidez matutina.",
            postureInstructions = listOf(
                "Mantén la parte baja de los dedos (nudillos) extendida.",
                "Dobla únicamente las dos articulaciones superiores formando un gancho suave.",
                "Vuelve a estirar lentamente sintiendo la apertura de la palma."
            ),
            phases = listOf(
                MotionPhase("Formar gancho", "Dobla solo las puntas manteniendo nudillos rectos", 3, 1f),
                MotionPhase("Sostener", "Mantén la garra suave dos segundos", 2, 1f),
                MotionPhase("Estirar dedos", "Extiende suavemente hasta alinear los dedos", 3, 0f)
            ),
            avoidWarnings = listOf(
                "No cierres por completo la palma si el objetivo es aislar el gancho.",
                "Detente ante sensación de tirón agudo."
            ),
            recommendedForGoals = listOf(PrimaryGoal.MOVILIDAD, PrimaryGoal.RECUPERACION_SUAVE),
            visionTargetPoseName = "Dedos en posición de gancho con nudillos extendidos"
        ),
        RehabExercise(
            id = "finger_fan_abduction",
            title = "Abanico y cierre de dedos",
            category = ExerciseCategory.MOVILIDAD_DEDOS,
            motionType = HandMotionType.FINGER_SPREAD,
            difficultyLabel = "Suave",
            suggestedSets = 3,
            suggestedReps = 10,
            suggestedMinutes = 3,
            summary = "Separación amplia y retorno simétrico de los dedos para movilizar la musculatura intrínseca de la palma.",
            postureInstructions = listOf(
                "Apoya el dorso de la mano o la palma cómodamente sobre la mesa.",
                "Separa los cinco dedos todo lo que resulte cómodo, sin dolor.",
                "Vuelve a juntar los dedos lentamente hasta tocar sus bordes."
            ),
            phases = listOf(
                MotionPhase("Abrir abanico", "Separa ampliamente todos los dedos", 3, 1f),
                MotionPhase("Juntar dedos", "Vuelve a juntar los dedos con suavidad", 3, 0f)
            ),
            avoidWarnings = listOf(
                "Evita doblar las puntas mientras separas los dedos.",
                "Respira con normalidad durante toda la serie."
            ),
            recommendedForGoals = listOf(PrimaryGoal.RECUPERACION_SUAVE, PrimaryGoal.MOVILIDAD),
            visionTargetPoseName = "Mano abierta con dedos separados simétricamente"
        )
    )

    fun findById(id: String): RehabExercise? = allExercises.firstOrNull { it.id == id }

    fun recommendedForGoal(goal: PrimaryGoal): List<RehabExercise> {
        val primary = allExercises.filter { goal in it.recommendedForGoals }
        return if (primary.size >= 4) primary.take(4) else allExercises.take(4)
    }
}

object EbookCatalog {
    val chapters: List<EbookChapter> = listOf(
        EbookChapter(
            id = "cap_1",
            number = 1,
            title = "Cómo usar esta guía de rehabilitación de mano",
            subtitle = "Principios de seguridad, ritmo y autocuidado",
            readMinutes = 3,
            keyTakeaways = listOf(
                "La constancia suave supera siempre a la intensidad excesiva.",
                "Cada mano y cada lesión tiene tiempos propios de recuperación.",
                "Esta guía educativa acompaña tu práctica, pero no reemplaza una consulta presencial."
            ),
            bodyParagraphs = listOf(
                "Recuperar la movilidad, la fuerza o la destreza de la mano requiere paciencia y movimientos conscientes. Antes de iniciar cualquier serie, prepara un espacio tranquilo, apoya el antebrazo sobre una superficie cómoda y respira profundo.",
                "El criterio fundamental es la regla del confort controlado: puedes percibir una sensación suave de estiramiento o trabajo muscular, pero nunca dolor agudo, punzante, hormigueo nuevo ni inflamación posterior."
            ),
            safetyNote = "Si tuviste cirugía reciente, fractura inmovilizada o dolor agudo, consulta con tu kinesiólogo o médico antes de elegir tu rutina."
        ),
        EbookChapter(
            id = "cap_2",
            number = 2,
            title = "Postura base y preparación de la muñeca",
            subtitle = "Alineación del hombro, codo y antebrazo",
            readMinutes = 4,
            keyTakeaways = listOf(
                "Apoyar el antebrazo reduce compensaciones en el hombro y cuello.",
                "La muñeca neutra protege el túnel carpiano durante el movimiento de dedos.",
                "El calor suave previo (según indicación profesional) puede ayudar ante rigidez matutina."
            ),
            bodyParagraphs = listOf(
                "Muchos ejercicios fallan no por el movimiento de los dedos, sino porque el hombro o el cuello se tensan para compensar la falta de movilidad. Sentarse con la espalda apoyada y el antebrazo firme sobre una mesa cambia por completo la calidad del gesto.",
                "Observa tu muñeca como un puente recto entre el antebrazo y los nudillos. Evita dejarla caer hacia los lados mientras practicas ejercicios de motricidad fina o pinza."
            )
        ),
        EbookChapter(
            id = "cap_3",
            number = 3,
            title = "Deslizamiento tendinoso y movilidad de dedos",
            subtitle = "Secuencia progresiva sin carga",
            readMinutes = 4,
            keyTakeaways = listOf(
                "La secuencia recta → mesa → gancho → puño plano moviliza planos distintos.",
                "Las pausas de 2 segundos en cada postura favorecen el control neuromuscular.",
                "Menos repeticiones bien hechas valen más que series rápidas."
            ),
            bodyParagraphs = listOf(
                "Los tendones flexores recorren túneles estrechos desde el antebrazo hasta las puntas de los dedos. Alternar posiciones como la mesa (flexión de nudillos), el gancho y el puño plano ayuda a que cada tramo se deslice de forma armónica.",
                "Mantén un ritmo pausado: cuenta mentalmente o activa la guía de voz en español dentro de cada ficha de ejercicio."
            )
        ),
        EbookChapter(
            id = "cap_4",
            number = 4,
            title = "Motricidad fina y actividades diarias",
            subtitle = "Trasladar la práctica a gestos reales",
            readMinutes = 3,
            keyTakeaways = listOf(
                "La pinza redondeada protege la articulación del pulgar.",
                "Practicar 5 minutos con objetos livianos mejora la confianza cotidiana.",
                "Alterna descansos breves para evitar fatiga de los músculos pequeños."
            ),
            bodyParagraphs = listOf(
                "Abotonar una camisa, sostener una taza o girar una llave dependen de la coordinación entre el pulgar y el resto de los dedos. Entrenar la oposición formando círculos amplios prepara la mano sin sobrecarga.",
                "Cuando realices ejercicios con objetos pequeños, elige piezas livianas y presta atención a la suavidad al soltar, no solo al agarrar."
            )
        ),
        EbookChapter(
            id = "cap_5",
            number = 5,
            title = "Cuándo detener un ejercicio y señales de alerta",
            subtitle = "Criterios responsables de seguridad",
            readMinutes = 3,
            keyTakeaways = listOf(
                "Detenerse a tiempo previene retrocesos e inflamación.",
                "Registra tu nivel de molestia al terminar cada sesión para detectar tendencias.",
                "Ante síntomas nuevos, prioriza una valoración profesional."
            ),
            bodyParagraphs = listOf(
                "Detén la sesión de inmediato si aparece dolor agudo nuevo, sensación de corriente eléctrica, entumecimiento persistente, cambio marcado de coloración, aumento de hinchazón o mareos.",
                "El registro en 'Mi progreso' permite observar si la molestia baja semana a semana o si conviene ajustar el volumen junto a tu especialista."
            ),
            safetyNote = "Rehab Manos AI es una plataforma educativa y de acompañamiento. No realiza diagnósticos médicos ni sustituye evaluaciones presenciales."
        )
    )
}
