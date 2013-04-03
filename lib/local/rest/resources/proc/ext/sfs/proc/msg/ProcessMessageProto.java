// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: processmsg.proto

package sfs.proc.msg;

public final class ProcessMessageProto {
  private ProcessMessageProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface ProcessMessageOrBuilder
      extends com.google.protobuf.MessageOrBuilder {
    
    // optional string procname = 1;
    boolean hasProcname();
    String getProcname();
    
    // optional string path = 2;
    boolean hasPath();
    String getPath();
    
    // optional string subid = 3;
    boolean hasSubid();
    String getSubid();
    
    // optional string data = 4;
    boolean hasData();
    String getData();
    
    // optional string pubid = 5;
    boolean hasPubid();
    String getPubid();
    
    // required .sfs.proc.msg.ProcessMessage.ProcessMessageType type = 6;
    boolean hasType();
    sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType getType();
    
    // optional string key = 7;
    boolean hasKey();
    String getKey();
  }
  public static final class ProcessMessage extends
      com.google.protobuf.GeneratedMessage
      implements ProcessMessageOrBuilder {
    // Use ProcessMessage.newBuilder() to construct.
    private ProcessMessage(Builder builder) {
      super(builder);
    }
    private ProcessMessage(boolean noInit) {}
    
    private static final ProcessMessage defaultInstance;
    public static ProcessMessage getDefaultInstance() {
      return defaultInstance;
    }
    
    public ProcessMessage getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return sfs.proc.msg.ProcessMessageProto.internal_static_sfs_proc_msg_ProcessMessage_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return sfs.proc.msg.ProcessMessageProto.internal_static_sfs_proc_msg_ProcessMessage_fieldAccessorTable;
    }
    
    public enum ProcessMessageType
        implements com.google.protobuf.ProtocolMessageEnum {
      INSTALL(0, 0),
      DATA(1, 1),
      DESTROY(2, 2),
      PING(3, 3),
      START(4, 4),
      START_OK(5, 8),
      START_FAIL(6, 9),
      KEY(7, 5),
      INSTALL_OK(8, 6),
      INSTALL_FAILED(9, 7),
      ;
      
      public static final int INSTALL_VALUE = 0;
      public static final int DATA_VALUE = 1;
      public static final int DESTROY_VALUE = 2;
      public static final int PING_VALUE = 3;
      public static final int START_VALUE = 4;
      public static final int START_OK_VALUE = 8;
      public static final int START_FAIL_VALUE = 9;
      public static final int KEY_VALUE = 5;
      public static final int INSTALL_OK_VALUE = 6;
      public static final int INSTALL_FAILED_VALUE = 7;
      
      
      public final int getNumber() { return value; }
      
      public static ProcessMessageType valueOf(int value) {
        switch (value) {
          case 0: return INSTALL;
          case 1: return DATA;
          case 2: return DESTROY;
          case 3: return PING;
          case 4: return START;
          case 8: return START_OK;
          case 9: return START_FAIL;
          case 5: return KEY;
          case 6: return INSTALL_OK;
          case 7: return INSTALL_FAILED;
          default: return null;
        }
      }
      
      public static com.google.protobuf.Internal.EnumLiteMap<ProcessMessageType>
          internalGetValueMap() {
        return internalValueMap;
      }
      private static com.google.protobuf.Internal.EnumLiteMap<ProcessMessageType>
          internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<ProcessMessageType>() {
              public ProcessMessageType findValueByNumber(int number) {
                return ProcessMessageType.valueOf(number);
              }
            };
      
      public final com.google.protobuf.Descriptors.EnumValueDescriptor
          getValueDescriptor() {
        return getDescriptor().getValues().get(index);
      }
      public final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptorForType() {
        return getDescriptor();
      }
      public static final com.google.protobuf.Descriptors.EnumDescriptor
          getDescriptor() {
        return sfs.proc.msg.ProcessMessageProto.ProcessMessage.getDescriptor().getEnumTypes().get(0);
      }
      
      private static final ProcessMessageType[] VALUES = {
        INSTALL, DATA, DESTROY, PING, START, START_OK, START_FAIL, KEY, INSTALL_OK, INSTALL_FAILED, 
      };
      
      public static ProcessMessageType valueOf(
          com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
          throw new java.lang.IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
        }
        return VALUES[desc.getIndex()];
      }
      
      private final int index;
      private final int value;
      
      private ProcessMessageType(int index, int value) {
        this.index = index;
        this.value = value;
      }
      
      // @@protoc_insertion_point(enum_scope:sfs.proc.msg.ProcessMessage.ProcessMessageType)
    }
    
    private int bitField0_;
    // optional string procname = 1;
    public static final int PROCNAME_FIELD_NUMBER = 1;
    private java.lang.Object procname_;
    public boolean hasProcname() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    public String getProcname() {
      java.lang.Object ref = procname_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          procname_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getProcnameBytes() {
      java.lang.Object ref = procname_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        procname_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    // optional string path = 2;
    public static final int PATH_FIELD_NUMBER = 2;
    private java.lang.Object path_;
    public boolean hasPath() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    public String getPath() {
      java.lang.Object ref = path_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          path_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getPathBytes() {
      java.lang.Object ref = path_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        path_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    // optional string subid = 3;
    public static final int SUBID_FIELD_NUMBER = 3;
    private java.lang.Object subid_;
    public boolean hasSubid() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    public String getSubid() {
      java.lang.Object ref = subid_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          subid_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getSubidBytes() {
      java.lang.Object ref = subid_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        subid_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    // optional string data = 4;
    public static final int DATA_FIELD_NUMBER = 4;
    private java.lang.Object data_;
    public boolean hasData() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    public String getData() {
      java.lang.Object ref = data_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          data_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getDataBytes() {
      java.lang.Object ref = data_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        data_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    // optional string pubid = 5;
    public static final int PUBID_FIELD_NUMBER = 5;
    private java.lang.Object pubid_;
    public boolean hasPubid() {
      return ((bitField0_ & 0x00000010) == 0x00000010);
    }
    public String getPubid() {
      java.lang.Object ref = pubid_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          pubid_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getPubidBytes() {
      java.lang.Object ref = pubid_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        pubid_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    // required .sfs.proc.msg.ProcessMessage.ProcessMessageType type = 6;
    public static final int TYPE_FIELD_NUMBER = 6;
    private sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType type_;
    public boolean hasType() {
      return ((bitField0_ & 0x00000020) == 0x00000020);
    }
    public sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType getType() {
      return type_;
    }
    
    // optional string key = 7;
    public static final int KEY_FIELD_NUMBER = 7;
    private java.lang.Object key_;
    public boolean hasKey() {
      return ((bitField0_ & 0x00000040) == 0x00000040);
    }
    public String getKey() {
      java.lang.Object ref = key_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        if (com.google.protobuf.Internal.isValidUtf8(bs)) {
          key_ = s;
        }
        return s;
      }
    }
    private com.google.protobuf.ByteString getKeyBytes() {
      java.lang.Object ref = key_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        key_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    
    private void initFields() {
      procname_ = "";
      path_ = "";
      subid_ = "";
      data_ = "";
      pubid_ = "";
      type_ = sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType.INSTALL;
      key_ = "";
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;
      
      if (!hasType()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, getProcnameBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeBytes(2, getPathBytes());
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeBytes(3, getSubidBytes());
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        output.writeBytes(4, getDataBytes());
      }
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        output.writeBytes(5, getPubidBytes());
      }
      if (((bitField0_ & 0x00000020) == 0x00000020)) {
        output.writeEnum(6, type_.getNumber());
      }
      if (((bitField0_ & 0x00000040) == 0x00000040)) {
        output.writeBytes(7, getKeyBytes());
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getProcnameBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, getPathBytes());
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(3, getSubidBytes());
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(4, getDataBytes());
      }
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(5, getPubidBytes());
      }
      if (((bitField0_ & 0x00000020) == 0x00000020)) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(6, type_.getNumber());
      }
      if (((bitField0_ & 0x00000040) == 0x00000040)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(7, getKeyBytes());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }
    
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static sfs.proc.msg.ProcessMessageProto.ProcessMessage parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(sfs.proc.msg.ProcessMessageProto.ProcessMessage prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements sfs.proc.msg.ProcessMessageProto.ProcessMessageOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return sfs.proc.msg.ProcessMessageProto.internal_static_sfs_proc_msg_ProcessMessage_descriptor;
      }
      
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return sfs.proc.msg.ProcessMessageProto.internal_static_sfs_proc_msg_ProcessMessage_fieldAccessorTable;
      }
      
      // Construct using sfs.proc.msg.ProcessMessageProto.ProcessMessage.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }
      
      private Builder(BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }
      
      public Builder clear() {
        super.clear();
        procname_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        path_ = "";
        bitField0_ = (bitField0_ & ~0x00000002);
        subid_ = "";
        bitField0_ = (bitField0_ & ~0x00000004);
        data_ = "";
        bitField0_ = (bitField0_ & ~0x00000008);
        pubid_ = "";
        bitField0_ = (bitField0_ & ~0x00000010);
        type_ = sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType.INSTALL;
        bitField0_ = (bitField0_ & ~0x00000020);
        key_ = "";
        bitField0_ = (bitField0_ & ~0x00000040);
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return sfs.proc.msg.ProcessMessageProto.ProcessMessage.getDescriptor();
      }
      
      public sfs.proc.msg.ProcessMessageProto.ProcessMessage getDefaultInstanceForType() {
        return sfs.proc.msg.ProcessMessageProto.ProcessMessage.getDefaultInstance();
      }
      
      public sfs.proc.msg.ProcessMessageProto.ProcessMessage build() {
        sfs.proc.msg.ProcessMessageProto.ProcessMessage result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }
      
      private sfs.proc.msg.ProcessMessageProto.ProcessMessage buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        sfs.proc.msg.ProcessMessageProto.ProcessMessage result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return result;
      }
      
      public sfs.proc.msg.ProcessMessageProto.ProcessMessage buildPartial() {
        sfs.proc.msg.ProcessMessageProto.ProcessMessage result = new sfs.proc.msg.ProcessMessageProto.ProcessMessage(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.procname_ = procname_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.path_ = path_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.subid_ = subid_;
        if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
          to_bitField0_ |= 0x00000008;
        }
        result.data_ = data_;
        if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
          to_bitField0_ |= 0x00000010;
        }
        result.pubid_ = pubid_;
        if (((from_bitField0_ & 0x00000020) == 0x00000020)) {
          to_bitField0_ |= 0x00000020;
        }
        result.type_ = type_;
        if (((from_bitField0_ & 0x00000040) == 0x00000040)) {
          to_bitField0_ |= 0x00000040;
        }
        result.key_ = key_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof sfs.proc.msg.ProcessMessageProto.ProcessMessage) {
          return mergeFrom((sfs.proc.msg.ProcessMessageProto.ProcessMessage)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(sfs.proc.msg.ProcessMessageProto.ProcessMessage other) {
        if (other == sfs.proc.msg.ProcessMessageProto.ProcessMessage.getDefaultInstance()) return this;
        if (other.hasProcname()) {
          setProcname(other.getProcname());
        }
        if (other.hasPath()) {
          setPath(other.getPath());
        }
        if (other.hasSubid()) {
          setSubid(other.getSubid());
        }
        if (other.hasData()) {
          setData(other.getData());
        }
        if (other.hasPubid()) {
          setPubid(other.getPubid());
        }
        if (other.hasType()) {
          setType(other.getType());
        }
        if (other.hasKey()) {
          setKey(other.getKey());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      public final boolean isInitialized() {
        if (!hasType()) {
          
          return false;
        }
        return true;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              onChanged();
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                onChanged();
                return this;
              }
              break;
            }
            case 10: {
              bitField0_ |= 0x00000001;
              procname_ = input.readBytes();
              break;
            }
            case 18: {
              bitField0_ |= 0x00000002;
              path_ = input.readBytes();
              break;
            }
            case 26: {
              bitField0_ |= 0x00000004;
              subid_ = input.readBytes();
              break;
            }
            case 34: {
              bitField0_ |= 0x00000008;
              data_ = input.readBytes();
              break;
            }
            case 42: {
              bitField0_ |= 0x00000010;
              pubid_ = input.readBytes();
              break;
            }
            case 48: {
              int rawValue = input.readEnum();
              sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType value = sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType.valueOf(rawValue);
              if (value == null) {
                unknownFields.mergeVarintField(6, rawValue);
              } else {
                bitField0_ |= 0x00000020;
                type_ = value;
              }
              break;
            }
            case 58: {
              bitField0_ |= 0x00000040;
              key_ = input.readBytes();
              break;
            }
          }
        }
      }
      
      private int bitField0_;
      
      // optional string procname = 1;
      private java.lang.Object procname_ = "";
      public boolean hasProcname() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      public String getProcname() {
        java.lang.Object ref = procname_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          procname_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setProcname(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        procname_ = value;
        onChanged();
        return this;
      }
      public Builder clearProcname() {
        bitField0_ = (bitField0_ & ~0x00000001);
        procname_ = getDefaultInstance().getProcname();
        onChanged();
        return this;
      }
      void setProcname(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000001;
        procname_ = value;
        onChanged();
      }
      
      // optional string path = 2;
      private java.lang.Object path_ = "";
      public boolean hasPath() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      public String getPath() {
        java.lang.Object ref = path_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          path_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setPath(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        path_ = value;
        onChanged();
        return this;
      }
      public Builder clearPath() {
        bitField0_ = (bitField0_ & ~0x00000002);
        path_ = getDefaultInstance().getPath();
        onChanged();
        return this;
      }
      void setPath(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000002;
        path_ = value;
        onChanged();
      }
      
      // optional string subid = 3;
      private java.lang.Object subid_ = "";
      public boolean hasSubid() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      public String getSubid() {
        java.lang.Object ref = subid_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          subid_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setSubid(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000004;
        subid_ = value;
        onChanged();
        return this;
      }
      public Builder clearSubid() {
        bitField0_ = (bitField0_ & ~0x00000004);
        subid_ = getDefaultInstance().getSubid();
        onChanged();
        return this;
      }
      void setSubid(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000004;
        subid_ = value;
        onChanged();
      }
      
      // optional string data = 4;
      private java.lang.Object data_ = "";
      public boolean hasData() {
        return ((bitField0_ & 0x00000008) == 0x00000008);
      }
      public String getData() {
        java.lang.Object ref = data_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          data_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setData(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000008;
        data_ = value;
        onChanged();
        return this;
      }
      public Builder clearData() {
        bitField0_ = (bitField0_ & ~0x00000008);
        data_ = getDefaultInstance().getData();
        onChanged();
        return this;
      }
      void setData(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000008;
        data_ = value;
        onChanged();
      }
      
      // optional string pubid = 5;
      private java.lang.Object pubid_ = "";
      public boolean hasPubid() {
        return ((bitField0_ & 0x00000010) == 0x00000010);
      }
      public String getPubid() {
        java.lang.Object ref = pubid_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          pubid_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setPubid(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000010;
        pubid_ = value;
        onChanged();
        return this;
      }
      public Builder clearPubid() {
        bitField0_ = (bitField0_ & ~0x00000010);
        pubid_ = getDefaultInstance().getPubid();
        onChanged();
        return this;
      }
      void setPubid(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000010;
        pubid_ = value;
        onChanged();
      }
      
      // required .sfs.proc.msg.ProcessMessage.ProcessMessageType type = 6;
      private sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType type_ = sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType.INSTALL;
      public boolean hasType() {
        return ((bitField0_ & 0x00000020) == 0x00000020);
      }
      public sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType getType() {
        return type_;
      }
      public Builder setType(sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType value) {
        if (value == null) {
          throw new NullPointerException();
        }
        bitField0_ |= 0x00000020;
        type_ = value;
        onChanged();
        return this;
      }
      public Builder clearType() {
        bitField0_ = (bitField0_ & ~0x00000020);
        type_ = sfs.proc.msg.ProcessMessageProto.ProcessMessage.ProcessMessageType.INSTALL;
        onChanged();
        return this;
      }
      
      // optional string key = 7;
      private java.lang.Object key_ = "";
      public boolean hasKey() {
        return ((bitField0_ & 0x00000040) == 0x00000040);
      }
      public String getKey() {
        java.lang.Object ref = key_;
        if (!(ref instanceof String)) {
          String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
          key_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }
      public Builder setKey(String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000040;
        key_ = value;
        onChanged();
        return this;
      }
      public Builder clearKey() {
        bitField0_ = (bitField0_ & ~0x00000040);
        key_ = getDefaultInstance().getKey();
        onChanged();
        return this;
      }
      void setKey(com.google.protobuf.ByteString value) {
        bitField0_ |= 0x00000040;
        key_ = value;
        onChanged();
      }
      
      // @@protoc_insertion_point(builder_scope:sfs.proc.msg.ProcessMessage)
    }
    
    static {
      defaultInstance = new ProcessMessage(true);
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:sfs.proc.msg.ProcessMessage)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_sfs_proc_msg_ProcessMessage_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_sfs_proc_msg_ProcessMessage_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020processmsg.proto\022\014sfs.proc.msg\"\303\002\n\016Pro" +
      "cessMessage\022\020\n\010procname\030\001 \001(\t\022\014\n\004path\030\002 " +
      "\001(\t\022\r\n\005subid\030\003 \001(\t\022\014\n\004data\030\004 \001(\t\022\r\n\005pubi" +
      "d\030\005 \001(\t\022=\n\004type\030\006 \002(\0162/.sfs.proc.msg.Pro" +
      "cessMessage.ProcessMessageType\022\013\n\003key\030\007 " +
      "\001(\t\"\230\001\n\022ProcessMessageType\022\013\n\007INSTALL\020\000\022" +
      "\010\n\004DATA\020\001\022\013\n\007DESTROY\020\002\022\010\n\004PING\020\003\022\t\n\005STAR" +
      "T\020\004\022\014\n\010START_OK\020\010\022\016\n\nSTART_FAIL\020\t\022\007\n\003KEY" +
      "\020\005\022\016\n\nINSTALL_OK\020\006\022\022\n\016INSTALL_FAILED\020\007B\025" +
      "B\023ProcessMessageProto"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_sfs_proc_msg_ProcessMessage_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_sfs_proc_msg_ProcessMessage_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_sfs_proc_msg_ProcessMessage_descriptor,
              new java.lang.String[] { "Procname", "Path", "Subid", "Data", "Pubid", "Type", "Key", },
              sfs.proc.msg.ProcessMessageProto.ProcessMessage.class,
              sfs.proc.msg.ProcessMessageProto.ProcessMessage.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  // @@protoc_insertion_point(outer_class_scope)
}