package io.quarkus.kafka.streams.deployment;

import java.io.IOException;
import java.util.List;

import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.processor.DefaultPartitionGrouper;
import org.apache.kafka.streams.processor.FailOnInvalidTimestamp;
import org.apache.kafka.streams.processor.internals.StreamsPartitionAssignor;
import org.rocksdb.util.Environment;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.UnknownFieldSet;

import io.quarkus.deployment.JniProcessor.JniConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRecorder;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

class KafkaStreamsProcessor {

    KafkaConfig kafka;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static class KafkaConfig {
        
        /**
         * Enable Protocol Buffer support.
         */
        @ConfigItem(defaultValue = "false")
        boolean enableProtoBuf = false;
    }
    
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(RecorderContext recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized,
            BuildProducer<SubstrateResourceBuildItem> nativeLibs) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.KAFKA_STREAMS));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, StreamsPartitionAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultPartitionGrouper.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultProductionExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, LogAndFailExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, ByteArraySerde.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, FailOnInvalidTimestamp.class));

        // for RocksDB, either add linux64 native lib when targeting containers
        if (isContainerBuild()) {
            nativeLibs.produce(new SubstrateResourceBuildItem("librocksdbjni-linux64.so"));
        }
        // otherwise the native lib of the platform this build runs on
        else {
            nativeLibs.produce(new SubstrateResourceBuildItem(Environment.getJniLibraryFileName("rocksdb")));
        }

        // re-initializing RocksDB to enable load of native libs
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("org.rocksdb.RocksDB"));
        
        if(kafka.enableProtoBuf) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, AbstractMessage.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, AbstractMessage.Builder.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, AbstractMessageLite.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, AbstractMessageLite.Builder.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, ByteString.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, Descriptors.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, Descriptors.Descriptor.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, GeneratedMessageV3.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, GeneratedMessageV3.Builder.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, GeneratedMessageV3.FieldAccessorTable.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, Message.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, Message.Builder.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, MessageLite.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, MessageLite.Builder.class));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, UnknownFieldSet.class));
            
            /*
            - com.google.protobuf.GeneratedMessageV3$FieldAccessorTable
            - com.google.protobuf.Message
            - com.google.protobuf.Message$Builder
            - com.google.protobuf.MessageLite
            - com.google.protobuf.MessageLite$Builder
            - com.google.protobuf.UnknownFieldSet
            */
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void build(KafkaStreamsRecorder recorder) {
        // Explicitly loading RocksDB native libs, as that's normally done from within
        // static initializers which already ran during build
        recorder.loadRocksDb();
    }

    private boolean isContainerBuild() {
        String containerRuntime = System.getProperty("native-image.container-runtime");

        if (containerRuntime != null) {
            containerRuntime = containerRuntime.trim().toLowerCase();
            return containerRuntime.equals("docker") || containerRuntime.equals("podman");
        }

        String dockerBuild = System.getProperty("native-image.docker-build");

        if (dockerBuild != null) {
            dockerBuild = dockerBuild.trim().toLowerCase();
            return !"false".equals(dockerBuild);
        }

        return false;
    }
}
