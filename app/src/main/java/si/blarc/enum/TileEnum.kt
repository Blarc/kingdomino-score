package si.blarc.enum

enum class TileEnum(val label: String) {
    CENTER("center"),
    FOREST_0("forest0"),
    FOREST_1("forest1"),
    GRASS_0("grass0"),
    GRASS_1("grass1"),
    GRASS_2("grass2"),
    GRASS_3("grass3"),
    MINE_1("mine1"),
    MINE_2("mine2"),
    MINE_3("mine3"),
    SWAMP_0("swamp0"),
    SWAMP_1("swamp1"),
    SWAMP_2("swamp2"),
    WATER_0("water0"),
    WATER_1("water1"),
    WHEAT_0("wheat0"),
    WHEAT_1("wheat1");

    companion object {
        fun fromString(label: String): TileEnum {
            for (tileEnum in values()) {
                if (tileEnum.label == label) {
                    return tileEnum
                }
            }
            throw IllegalArgumentException("No enum with label $label found")
        }
    }
}