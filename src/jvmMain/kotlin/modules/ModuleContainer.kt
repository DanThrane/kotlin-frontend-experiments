package dk.thrane.playground.modules

import dk.thrane.playground.AttributeStore
import dk.thrane.playground.BaseServer
import dk.thrane.playground.Log
import dk.thrane.playground.startServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ModuleContainer(val cliArgs: List<String>) : BaseServer() {
    private val plugins = AttributeStore()
    val attributes = AttributeStore()
    internal val modules = ArrayList<Module>()
    private val onStartScripts = ArrayList<suspend () -> Unit>()

    fun addOnStartScript(script: suspend () -> Unit) {
        onStartScripts.add(script)
    }

    fun <Plugin : ContainerPlugin> install(pluginFactory: ContainerPluginFactory<Plugin>) {
        log.info("Installing plugin: ${pluginFactory.key.name}")
        val plugin = pluginFactory.createPlugin(this)
        plugins[pluginFactory.key] = plugin
    }

    fun <Plugin : ContainerPlugin> getPlugin(factory: ContainerPluginFactory<Plugin>): Plugin {
        return plugins[factory.key]
    }

    fun <Plugin : ContainerPlugin> getPluginOrNull(factory: ContainerPluginFactory<Plugin>): Plugin? {
        return plugins.getOrNull(factory.key)
    }

    fun install(module: Module) {
        log.info("Installing module: ${module.javaClass.simpleName}")
        modules.add(module)
    }

    fun install(classPath: String) {
        log.info("Installing module at $classPath")
        val loadedClass = try {
            ModuleContainer::class.java.classLoader.loadClass(classPath).kotlin
        } catch (ex: ClassNotFoundException) {
            // TODO Output the current classpath
            throw IllegalArgumentException("Class was not found! We were looking for '$classPath'", ex)
        }

        val objectInstance = loadedClass.objectInstance
        if (objectInstance != null) {
            if (objectInstance !is Module) {
                throw IllegalArgumentException("Loaded class '$classPath' ($objectInstance) is not a module")
            }

            modules.add(objectInstance)
        } else {
            val constructor = loadedClass.constructors.find { constructor ->
                constructor.parameters.all { param -> param.isOptional }
            } ?: throw IllegalArgumentException("Unable to find empty Module constructor for '$classPath'")

            val moduleInstance = constructor.call()
            if (moduleInstance !is Module) {
                throw IllegalArgumentException("Loaded class '$classPath' ($moduleInstance) is not a module")
            }

            modules.add(moduleInstance)
        }
    }

    fun start(): Job {
        modules.forEach { module ->
            module.init(this)
            module.controllers.forEach { addController(it) }
        }

        runBlocking {
            onStartScripts.forEach { it() }
        }

        return GlobalScope.launch {
            startServer(httpRequestHandler = this@ModuleContainer, webSocketRequestHandler = this@ModuleContainer)
        }
    }

    companion object {
        private val log = Log("ModuleContainer")
    }
}
