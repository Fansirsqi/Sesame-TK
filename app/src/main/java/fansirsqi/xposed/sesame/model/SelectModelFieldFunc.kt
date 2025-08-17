package fansirsqi.xposed.sesame.model

interface SelectModelFieldFunc {
    fun clear()
    fun get(id: String?): Int?
    fun add(id: String?, count: Int?)
    fun remove(id: String?)
    fun contains(id: String?): Boolean?

    companion object {
        @JvmStatic
        fun newMapInstance(): SelectModelFieldFunc {
            return object : SelectModelFieldFunc {
                private val map: MutableMap<String?, Int?> = LinkedHashMap<String?, Int?>()
                override fun clear() {
                    map.clear()
                }

                override fun get(id: String?): Int? {
                    return map.get(id)
                }

                override fun add(id: String?, count: Int?) {
                    map.put(id, count)
                }

                override fun remove(id: String?) {
                    map.remove(id)
                }

                override fun contains(id: String?): Boolean {
                    return map.containsKey(id)
                }
            }
        }
    }
}