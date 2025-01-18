import {PlayerDto} from '../../../../api/generated/models/player-dto';

export interface Relationship {
  friendsList: PlayerDto[],
  blockedList: PlayerDto[]
}
