//
// Copyright (c) ZeroC, Inc. All rights reserved.
//
//
// Ice version 3.7.10
//
// <auto-generated>
//
// Generated from file `ReportsSystem.ice'
//
// Warning: do not edit this file.
//
// </auto-generated>
//

package ReportsSystem;

public interface ReportsService extends com.zeroc.Ice.Object
{
    /**
     * Obtiene el reporte completo de un ciudadano
     * @param current The Current object for the invocation.
     **/
    String getCitizenReports(String documento, int electionId, com.zeroc.Ice.Current current);

    /**
     * Busca ciudadanos por nombre y apellido
     * @param current The Current object for the invocation.
     **/
    String[] searchCitizenReports(String nombre, String apellido, int electionId, int limit, com.zeroc.Ice.Current current);

    /**
     * Obtiene todos los ciudadanos de una mesa
     * @param current The Current object for the invocation.
     **/
    String[] getMesaCitizenReports(int mesaId, int electionId, com.zeroc.Ice.Current current);

    /**
     * Valida si un ciudadano es elegible para votar
     * @param current The Current object for the invocation.
     **/
    boolean validateCitizenEligibility(String documento, com.zeroc.Ice.Current current);

    /**
     * Obtiene el reporte de resultados de una elección
     * @param current The Current object for the invocation.
     **/
    String getElectionReports(int electionId, com.zeroc.Ice.Current current);

    /**
     * Obtiene la lista de elecciones disponibles
     * @param current The Current object for the invocation.
     **/
    String[] getAvailableElections(com.zeroc.Ice.Current current);

    /**
     * Verifica si los reportes están listos para una elección
     * @param current The Current object for the invocation.
     **/
    boolean areReportsReady(int electionId, com.zeroc.Ice.Current current);

    /**
     * Obtiene reportes geográficos (departamento, municipio, puesto)
     * @param current The Current object for the invocation.
     **/
    String getGeographicReports(int locationId, String locationType, int electionId, com.zeroc.Ice.Current current);

    /**
     * Precarga reportes de manera inteligente según el tipo
     * @param current The Current object for the invocation.
     **/
    String preloadReports(int electionId, String locationType, int locationId, com.zeroc.Ice.Current current);

    /**
     * Obtiene estadísticas del cache
     * @param current The Current object for the invocation.
     **/
    String getCacheStats(com.zeroc.Ice.Current current);

    /**
     * Obtiene los documentos de todos los ciudadanos de un departamento
     * @param current The Current object for the invocation.
     **/
    String[] getDepartmentCitizenDocuments(int departmentId, int electionId, com.zeroc.Ice.Current current);

    /**
     * Obtiene los documentos de todos los ciudadanos de un municipio
     * @param current The Current object for the invocation.
     **/
    String[] getMunicipalityCitizenDocuments(int municipalityId, int electionId, com.zeroc.Ice.Current current);

    /**
     * Obtiene los documentos de todos los ciudadanos de un puesto
     * @param current The Current object for the invocation.
     **/
    String[] getPuestoCitizenDocuments(int puestoId, int electionId, com.zeroc.Ice.Current current);

    /**
     * Obtiene los documentos de ciudadanos de una mesa específica
     * @param current The Current object for the invocation.
     **/
    String[] getMesaCitizenDocuments(int mesaId, int electionId, com.zeroc.Ice.Current current);

    /** @hidden */
    static final String[] _iceIds =
    {
        "::Ice::Object",
        "::ReportsSystem::ReportsService"
    };

    @Override
    default String[] ice_ids(com.zeroc.Ice.Current current)
    {
        return _iceIds;
    }

    @Override
    default String ice_id(com.zeroc.Ice.Current current)
    {
        return ice_staticId();
    }

    static String ice_staticId()
    {
        return "::ReportsSystem::ReportsService";
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getCitizenReports(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_documento;
        int iceP_electionId;
        iceP_documento = istr.readString();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String ret = obj.getCitizenReports(iceP_documento, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeString(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_searchCitizenReports(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_nombre;
        String iceP_apellido;
        int iceP_electionId;
        int iceP_limit;
        iceP_nombre = istr.readString();
        iceP_apellido = istr.readString();
        iceP_electionId = istr.readInt();
        iceP_limit = istr.readInt();
        inS.endReadParams();
        String[] ret = obj.searchCitizenReports(iceP_nombre, iceP_apellido, iceP_electionId, iceP_limit, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getMesaCitizenReports(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_mesaId;
        int iceP_electionId;
        iceP_mesaId = istr.readInt();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String[] ret = obj.getMesaCitizenReports(iceP_mesaId, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_validateCitizenEligibility(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        String iceP_documento;
        iceP_documento = istr.readString();
        inS.endReadParams();
        boolean ret = obj.validateCitizenEligibility(iceP_documento, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeBool(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getElectionReports(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_electionId;
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String ret = obj.getElectionReports(iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeString(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getAvailableElections(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        inS.readEmptyParams();
        String[] ret = obj.getAvailableElections(current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_areReportsReady(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_electionId;
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        boolean ret = obj.areReportsReady(iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeBool(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getGeographicReports(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_locationId;
        String iceP_locationType;
        int iceP_electionId;
        iceP_locationId = istr.readInt();
        iceP_locationType = istr.readString();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String ret = obj.getGeographicReports(iceP_locationId, iceP_locationType, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeString(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_preloadReports(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_electionId;
        String iceP_locationType;
        int iceP_locationId;
        iceP_electionId = istr.readInt();
        iceP_locationType = istr.readString();
        iceP_locationId = istr.readInt();
        inS.endReadParams();
        String ret = obj.preloadReports(iceP_electionId, iceP_locationType, iceP_locationId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeString(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getCacheStats(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        inS.readEmptyParams();
        String ret = obj.getCacheStats(current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeString(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getDepartmentCitizenDocuments(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_departmentId;
        int iceP_electionId;
        iceP_departmentId = istr.readInt();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String[] ret = obj.getDepartmentCitizenDocuments(iceP_departmentId, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getMunicipalityCitizenDocuments(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_municipalityId;
        int iceP_electionId;
        iceP_municipalityId = istr.readInt();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String[] ret = obj.getMunicipalityCitizenDocuments(iceP_municipalityId, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getPuestoCitizenDocuments(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_puestoId;
        int iceP_electionId;
        iceP_puestoId = istr.readInt();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String[] ret = obj.getPuestoCitizenDocuments(iceP_puestoId, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /**
     * @hidden
     * @param obj -
     * @param inS -
     * @param current -
     * @return -
    **/
    static java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceD_getMesaCitizenDocuments(ReportsService obj, final com.zeroc.IceInternal.Incoming inS, com.zeroc.Ice.Current current)
    {
        com.zeroc.Ice.Object._iceCheckMode(null, current.mode);
        com.zeroc.Ice.InputStream istr = inS.startReadParams();
        int iceP_mesaId;
        int iceP_electionId;
        iceP_mesaId = istr.readInt();
        iceP_electionId = istr.readInt();
        inS.endReadParams();
        String[] ret = obj.getMesaCitizenDocuments(iceP_mesaId, iceP_electionId, current);
        com.zeroc.Ice.OutputStream ostr = inS.startWriteParams();
        ostr.writeStringSeq(ret);
        inS.endWriteParams(ostr);
        return inS.setResult(ostr);
    }

    /** @hidden */
    final static String[] _iceOps =
    {
        "areReportsReady",
        "getAvailableElections",
        "getCacheStats",
        "getCitizenReports",
        "getDepartmentCitizenDocuments",
        "getElectionReports",
        "getGeographicReports",
        "getMesaCitizenDocuments",
        "getMesaCitizenReports",
        "getMunicipalityCitizenDocuments",
        "getPuestoCitizenDocuments",
        "ice_id",
        "ice_ids",
        "ice_isA",
        "ice_ping",
        "preloadReports",
        "searchCitizenReports",
        "validateCitizenEligibility"
    };

    /** @hidden */
    @Override
    default java.util.concurrent.CompletionStage<com.zeroc.Ice.OutputStream> _iceDispatch(com.zeroc.IceInternal.Incoming in, com.zeroc.Ice.Current current)
        throws com.zeroc.Ice.UserException
    {
        int pos = java.util.Arrays.binarySearch(_iceOps, current.operation);
        if(pos < 0)
        {
            throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
        }

        switch(pos)
        {
            case 0:
            {
                return _iceD_areReportsReady(this, in, current);
            }
            case 1:
            {
                return _iceD_getAvailableElections(this, in, current);
            }
            case 2:
            {
                return _iceD_getCacheStats(this, in, current);
            }
            case 3:
            {
                return _iceD_getCitizenReports(this, in, current);
            }
            case 4:
            {
                return _iceD_getDepartmentCitizenDocuments(this, in, current);
            }
            case 5:
            {
                return _iceD_getElectionReports(this, in, current);
            }
            case 6:
            {
                return _iceD_getGeographicReports(this, in, current);
            }
            case 7:
            {
                return _iceD_getMesaCitizenDocuments(this, in, current);
            }
            case 8:
            {
                return _iceD_getMesaCitizenReports(this, in, current);
            }
            case 9:
            {
                return _iceD_getMunicipalityCitizenDocuments(this, in, current);
            }
            case 10:
            {
                return _iceD_getPuestoCitizenDocuments(this, in, current);
            }
            case 11:
            {
                return com.zeroc.Ice.Object._iceD_ice_id(this, in, current);
            }
            case 12:
            {
                return com.zeroc.Ice.Object._iceD_ice_ids(this, in, current);
            }
            case 13:
            {
                return com.zeroc.Ice.Object._iceD_ice_isA(this, in, current);
            }
            case 14:
            {
                return com.zeroc.Ice.Object._iceD_ice_ping(this, in, current);
            }
            case 15:
            {
                return _iceD_preloadReports(this, in, current);
            }
            case 16:
            {
                return _iceD_searchCitizenReports(this, in, current);
            }
            case 17:
            {
                return _iceD_validateCitizenEligibility(this, in, current);
            }
        }

        assert(false);
        throw new com.zeroc.Ice.OperationNotExistException(current.id, current.facet, current.operation);
    }
}
