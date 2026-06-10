package com.personalmorningalarm.data

/**
 * Preset stretch routines seeded on first launch. Exercises carry no routineId
 * here — the seeder assigns it (and the display order) after inserting each
 * routine. See [PersonalMorningAlarmApp].
 */
object StretchSeedData {

    data class SeedExercise(
        val name: String,
        val durationSeconds: Int,
        val instructions: String
    )

    data class SeedRoutine(
        val name: String,
        val exercises: List<SeedExercise>
    )

    /** "General Morning Stretch" is seeded as the default active routine (index 1). */
    const val DEFAULT_ACTIVE_INDEX = 1

    val routines: List<SeedRoutine> = listOf(
        SeedRoutine(
            name = "Pre-Run Warmup",
            exercises = listOf(
                SeedExercise("Leg swings", 30, "Hold a wall for balance and swing each leg forward and back."),
                SeedExercise("Hip circles", 30, "Hands on hips, draw slow circles with your hips each way."),
                SeedExercise("Arm circles", 30, "Arms out to the sides, make small then larger circles."),
                SeedExercise("Walking lunges", 45, "Step forward into a lunge, alternating legs as you go."),
                SeedExercise("High knees", 30, "Jog on the spot driving your knees up toward your chest.")
            )
        ),
        SeedRoutine(
            name = "General Morning Stretch",
            exercises = listOf(
                SeedExercise("Neck rolls", 30, "Slowly roll your neck in a circle, then reverse."),
                SeedExercise("Shoulder stretch", 30, "Pull each arm across your chest and hold."),
                SeedExercise("Standing hamstring", 40, "Hinge at the hips and reach toward your toes."),
                SeedExercise("Quad stretch", 40, "Hold each ankle behind you, knees together."),
                SeedExercise("Side stretch", 30, "Reach overhead and lean gently to each side.")
            )
        ),
        SeedRoutine(
            name = "Rest Day Mobility",
            exercises = listOf(
                SeedExercise("Cat-cow", 45, "On all fours, alternate arching and rounding your back."),
                SeedExercise("Child's pose", 45, "Sit back onto your heels with arms stretched forward."),
                SeedExercise("Seated spinal twist", 40, "Sit tall and twist gently to each side."),
                SeedExercise("Figure-four stretch", 40, "Lying down, cross one ankle over the opposite knee and pull."),
                SeedExercise("Deep breathing", 30, "Slow breaths — in through the nose, out through the mouth.")
            )
        )
    )
}
