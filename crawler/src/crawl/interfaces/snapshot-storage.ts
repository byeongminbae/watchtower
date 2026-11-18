export interface SnapshotStorage {
  saveText(objectName: string, text: string): Promise<void>;
}
