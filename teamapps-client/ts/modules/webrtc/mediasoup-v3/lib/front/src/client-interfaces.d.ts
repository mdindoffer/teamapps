/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2020 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
import { MediaKind, RtpCapabilities, RtpEncodingParameters, RtpParameters } from 'mediasoup-client/lib/RtpParameters';
import { ProducerCodecOptions } from 'mediasoup-client/lib/Producer';
import { DtlsParameters } from 'mediasoup-client/lib/Transport';
export interface ConsumerData {
    consumerId: string;
}
export interface ConsumerPreferredLayers extends ConsumerData {
    layers: ConsumerLayers;
}
export interface ConsumerLayers {
    spatialLayer: number;
    temporalLayer?: number;
}
export interface ProducerData {
    producerId: string;
}
export interface ProduceRequest {
    transportId: string;
    stream: string;
    kind: MediaKind;
    rtpParameters: RtpParameters;
    paused?: boolean;
    keyFrameRequestDelay?: number;
    appData?: any;
}
export interface ProduceResponse {
    id: string;
}
export interface ConsumeResponse {
    producerId: string;
    id: string;
    kind: MediaKind;
    rtpParameters: RtpParameters;
    type: string;
    producerPaused: boolean;
}
export interface ConsumeRequestOriginData {
    token: string;
    from: string;
    to: string;
    origin?: ConsumeRequestOriginData;
}
export interface ConsumeRequest {
    origin?: ConsumeRequestOriginData;
    kind: MediaKind;
    stream: string;
    rtpCapabilities: RtpCapabilities;
    transportId: string;
}
export interface PipeToRemoteProducerRequest {
    origin: ConsumeRequestOriginData;
    kind: MediaKind;
    stream: string;
    localToken: string;
}
export interface PipeFromRemoteProducerRequest extends ProducerData {
    kind: MediaKind;
    stream: string;
    workerId: number;
}
export interface PipeTransportData {
    pipeTransportId: string;
    ip: string;
    port: number;
}
export interface PipeTransportConnectData extends PipeTransportData {
    transportId: string;
}
export interface WorkerLoadData {
    currentLoad: number;
}
export interface NumWorkersData {
    num: number;
}
export interface StatsInput {
    ids: string[];
}
export interface StatsOutput {
    [x: string]: {};
}
export interface TransportData {
    transportId: string;
}
export interface TransportBitrateData extends TransportData {
    bitrate: number;
}
export interface IceSever {
    urls: string[];
    username?: string;
    credential?: string;
}
export interface Simulcast {
    encodings?: RtpEncodingParameters[];
    codecOptions?: ProducerCodecOptions;
}
export interface ServerConfigs {
    routerRtpCapabilities: RtpCapabilities;
    iceServers?: IceSever[];
    simulcast?: Simulcast;
    timeout?: {
        stats: number;
        transport: number;
        consumer: number;
    };
}
export interface ConnectTransportRequest extends TransportData {
    transportId: string;
    dtlsParameters: DtlsParameters;
}
export interface RecordingData extends StreamKindsData {
}
export interface RecordingRequest extends StreamKindsData {
    layer?: number;
}
export interface KindsData {
    kinds: MediaKind[];
}
export interface StreamKindsData extends StreamData {
    kinds?: MediaKind[];
}
export interface StreamData {
    stream: string;
}
export interface StreamFileRequest extends StreamKindsData, KindsByFileInput {
    streamInput?: boolean;
    checkKinds?: boolean;
    additionalInputOptions?: string[];
    additionalOutputOptions?: string[];
}
export interface KindsByFileInput {
    filePath: string;
    relativePath?: boolean;
}
export interface PushStreamInputsResponse {
    options: string[];
}
export interface PushStreamInputsRequest extends PullStreamInputsRequest, PushStreamInputsResponse {
}
export interface TransportListenIp {
    ip: string;
    announcedIp?: string;
}
export interface PullStreamInputsRequest extends StreamKindsData {
    listenIp?: TransportListenIp | string;
    layer?: number;
}
export interface PullStreamInputsResponse {
    sdp: string;
    consumerIds: {
        [id: string]: string;
    };
}
export interface ConferenceInputOrigin {
    url: string;
    origin?: ConferenceInputOrigin;
    token?: string;
}
export interface ConferenceInput {
    stopTracks?: boolean;
    url?: string;
    origin?: ConferenceInputOrigin;
    stream: string;
    token: string;
    simulcast?: boolean;
    kinds?: MediaKind[];
    maxIncomingBitrate?: number;
    retryConsumerTimeout?: number;
}
export interface ConferenceConfig extends ConferenceInput {
    url: string;
    kinds: MediaKind[];
    maxIncomingBitrate: number;
    retryConsumerTimeout: number;
    timeout: {
        stats: number;
        transport: number;
        consumer: number;
    };
}
export interface ListData {
    list: string[];
}
export interface FilePathInput {
    filePath: string;
}
