package ru.plumsoftware.wordline.ui.game

import ru.plumsoftware.wordline.R

/** Normalized anchors measured on the painted trail; independent of the device's aspect ratio. */
internal data class MapArtwork(val resource: Int,val stops: List<Pair<Float,Float>>)

private fun mapArtwork(resource: Int,vararg stops: Pair<Int,Int>)=
    MapArtwork(resource,stops.map {(x,y)->x/327f to y/627f})

internal val mapArtworks=listOf(
    mapArtwork(R.drawable.map_lake_reference,204 to 449,139 to 380,96 to 310,144 to 254,192 to 182,262 to 126),
    mapArtwork(R.drawable.map_bamboo,249 to 445,174 to 380,113 to 310,95 to 250,176 to 190,260 to 132),
    mapArtwork(R.drawable.map_sahara,220 to 444,129 to 367,60 to 298,145 to 245,188 to 191,255 to 141),
    mapArtwork(R.drawable.map_moscow,230 to 449,183 to 375,107 to 321,117 to 260,201 to 215,222 to 160),
    mapArtwork(R.drawable.map_aurora,229 to 445,94 to 380,120 to 315,118 to 250,185 to 190,257 to 134),
    mapArtwork(R.drawable.map_mediterranean,222 to 445,249 to 380,143 to 325,128 to 255,185 to 200,250 to 143),
    mapArtwork(R.drawable.map_canyon,216 to 445,239 to 380,108 to 310,128 to 250,184 to 190,249 to 140),
    mapArtwork(R.drawable.map_tuscany,243 to 445,144 to 375,83 to 309,151 to 250,184 to 185,251 to 133),
    mapArtwork(R.drawable.map_tropics,222 to 448,249 to 380,124 to 326,101 to 258,186 to 197,250 to 133),
    mapArtwork(R.drawable.map_alps,191 to 445,210 to 380,43 to 310,128 to 250,203 to 200,248 to 140),
)
