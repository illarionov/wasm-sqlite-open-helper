-keepattributes SourceFile,LineNumberTable

-dontobfuscate

-keep @org.junit.Test class *
-keep class * {
    @org.junit.Test <methods>;
    @org.junit.BeforeTest <methods>;
    @org.junit.AfterTest <methods>;
    @org.junit.Ignore <methods>;
    @org.junit.Rule <methods>;
    @org.junit.Rule <fields>;
}

-keep class * extends ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractSqliteDriverTest {
    *;
}

-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.sun.nio.file.ExtendedOpenOption
-dontwarn java.lang.Module
-dontwarn java.lang.ModuleLayer
-dontwarn java.lang.Runtime$Version
-dontwarn java.lang.module.Configuration
-dontwarn java.lang.module.ModuleDescriptor$Builder
-dontwarn java.lang.module.ModuleDescriptor$Provides
-dontwarn java.lang.module.ModuleDescriptor$Requires$Modifier
-dontwarn java.lang.module.ModuleDescriptor$Requires
-dontwarn java.lang.module.ModuleDescriptor
-dontwarn java.lang.module.ModuleFinder
-dontwarn java.lang.module.ModuleReader
-dontwarn java.lang.module.ModuleReference
-dontwarn jdk.internal.access.JavaLangAccess
-dontwarn jdk.internal.access.SharedSecrets
-dontwarn jdk.internal.module.Modules
-dontwarn jdk.jfr.BooleanFlag
-dontwarn jdk.jfr.Category
-dontwarn jdk.jfr.DataAmount
-dontwarn jdk.jfr.Description
-dontwarn jdk.jfr.Event
-dontwarn jdk.jfr.FlightRecorder
-dontwarn jdk.jfr.FlightRecorderListener
-dontwarn jdk.jfr.Label
-dontwarn jdk.jfr.Name
-dontwarn jdk.jfr.Period
-dontwarn jdk.jfr.StackTrace
-dontwarn jdk.jfr.Timespan
-dontwarn jdk.jfr.Unsigned
-dontwarn jdk.vm.ci.code.InstalledCode
-dontwarn jdk.vm.ci.code.stack.InspectedFrame
-dontwarn jdk.vm.ci.code.stack.InspectedFrameVisitor
-dontwarn jdk.vm.ci.code.stack.StackIntrospection
-dontwarn jdk.vm.ci.common.JVMCIError
-dontwarn jdk.vm.ci.hotspot.HotSpotJVMCIRuntime
-dontwarn jdk.vm.ci.hotspot.HotSpotMetaAccessProvider
-dontwarn jdk.vm.ci.hotspot.HotSpotNmethod
-dontwarn jdk.vm.ci.hotspot.HotSpotObjectConstant
-dontwarn jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod
-dontwarn jdk.vm.ci.hotspot.HotSpotResolvedObjectType
-dontwarn jdk.vm.ci.hotspot.HotSpotSpeculationLog
-dontwarn jdk.vm.ci.hotspot.HotSpotVMConfigAccess
-dontwarn jdk.vm.ci.hotspot.HotSpotVMConfigStore
-dontwarn jdk.vm.ci.meta.JavaKind$FormatWithToString
-dontwarn jdk.vm.ci.meta.JavaKind
-dontwarn jdk.vm.ci.meta.MetaAccessProvider
-dontwarn jdk.vm.ci.meta.ResolvedJavaField
-dontwarn jdk.vm.ci.meta.ResolvedJavaMethod
-dontwarn jdk.vm.ci.meta.ResolvedJavaType
-dontwarn jdk.vm.ci.meta.SpeculationLog
-dontwarn jdk.vm.ci.runtime.JVMCI
-dontwarn jdk.vm.ci.runtime.JVMCIBackend
-dontwarn jdk.vm.ci.runtime.JVMCIRuntime
-dontwarn jdk.vm.ci.services.Services
-dontwarn kotlin.reflect.full.IllegalCallableAccessException
-dontwarn kotlin.reflect.full.KClasses
