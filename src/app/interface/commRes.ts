export interface CommonRes<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthorizationData {
  authorizationUrl: string;
}

export interface userInfo{
  id:number;
  mail:string;
  name:string;
}
