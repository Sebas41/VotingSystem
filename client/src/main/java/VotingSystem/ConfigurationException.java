//
// Copyright (c) ZeroC, Inc. All rights reserved.
//
//
// Ice version 3.7.10
//
// <auto-generated>
//
// Generated from file `VotingSystem.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package VotingSystem;

public class ConfigurationException extends com.zeroc.Ice.UserException
{
    public ConfigurationException()
    {
        this.reason = "";
    }

    public ConfigurationException(Throwable cause)
    {
        super(cause);
        this.reason = "";
    }

    public ConfigurationException(String reason, int errorCode)
    {
        this.reason = reason;
        this.errorCode = errorCode;
    }

    public ConfigurationException(String reason, int errorCode, Throwable cause)
    {
        super(cause);
        this.reason = reason;
        this.errorCode = errorCode;
    }

    public String ice_id()
    {
        return "::VotingSystem::ConfigurationException";
    }

    public String reason;

    public int errorCode;

    /** @hidden */
    @Override
    protected void _writeImpl(com.zeroc.Ice.OutputStream ostr_)
    {
        ostr_.startSlice("::VotingSystem::ConfigurationException", -1, true);
        ostr_.writeString(reason);
        ostr_.writeInt(errorCode);
        ostr_.endSlice();
    }

    /** @hidden */
    @Override
    protected void _readImpl(com.zeroc.Ice.InputStream istr_)
    {
        istr_.startSlice();
        reason = istr_.readString();
        errorCode = istr_.readInt();
        istr_.endSlice();
    }

    /** @hidden */
    public static final long serialVersionUID = 672126421L;
}
