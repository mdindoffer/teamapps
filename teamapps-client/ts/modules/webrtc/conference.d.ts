import { ConferenceData, Capturing } from './interfaces';
export declare class Conference {
    private readonly server_url;
    private readonly uid;
    private readonly token;
    private params;
    private videoContainer;
    private kind;
    private streamActiveTimeout;
    private _sendStream;
    private _adjustProfile;
    private ms;
    private ws;
    private room;
    private peers;
    private transport;
    private producers;
    private lastProduced;
    constructor(data: ConferenceData);
    private getPermissionsUrl;
    private request;
    private captureDevice;
    private setupRoom;
    private startSendStream;
    private startListenStream;
    protected __setVideoSource(videoContainer: HTMLMediaElement | null, streamFlow?: MediaStream): void;
    protected __whenStreamIsActive(getStream: Function, callback: Function): boolean;
    protected __hookup(capturing: Capturing): void;
    protected __doConnects(): void;
    protected __connectProducer(type: string, track: MediaStreamTrack | undefined): void;
    protected __startStream(peer: any): MediaStream;
    protected __makeAutoAdjustProfile(videoConsumer: any): Function;
    publish(): Promise<unknown>;
    play(): Promise<unknown>;
}
